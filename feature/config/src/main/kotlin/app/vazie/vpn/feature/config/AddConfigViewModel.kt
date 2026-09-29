package app.vazie.vpn.feature.config

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.feature.config.presentation.toDetection
import app.vazie.vpn.api.ProfileDraft
import app.vazie.vpn.api.ProfileOrigin
import app.vazie.vpn.api.SelectedProfileStore
import app.vazie.vpn.api.VpnProfileRepository
import app.vazie.vpn.config.ConfigParserRegistry
import app.vazie.vpn.config.ConfigSource
import app.vazie.vpn.config.ParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The wizard's draft, scoped to the add-configuration graph so the four steps share it. */
@HiltViewModel
class AddConfigViewModel @Inject constructor(
    private val parsers: ConfigParserRegistry,
    private val profiles: VpnProfileRepository,
    private val selected: SelectedProfileStore,
) : ViewModel() {

    // The on-screen formats come from the registry. See `ConfigParserRegistry.advertisedFormats`.
    private val _state = MutableStateFlow(
        AddConfigUiState(supportedFormats = parsers.advertisedFormats()),
    )
    val state: StateFlow<AddConfigUiState> = _state.asStateFlow()

    private val _effects = Channel<AddConfigEffect>(Channel.BUFFERED)
    val effects: Flow<AddConfigEffect> = _effects.receiveAsFlow()

    private var draft: ProfileDraft? = null

    fun onAction(action: AddConfigAction) {
        when (action) {
            is AddConfigAction.SelectMethod -> selectMethod(action.method)
            AddConfigAction.PasteRequested -> emit(AddConfigEffect.ReadClipboard)
            AddConfigAction.Continue -> continueToReview()
            is AddConfigAction.NameChanged -> _state.update { it.copy(name = action.name) }
            AddConfigAction.Save -> save()
            AddConfigAction.Close -> emit(AddConfigEffect.Close)
        }
    }

    /** A link typed or pasted into the first step's field: read it and show what came out. */
    fun onLinkSubmitted(text: Secret<String>) {
        if (text.expose().isBlank()) return
        draft = null
        _state.update { AddConfigUiState(method = ImportMethodUi.PASTE, supportedFormats = it.supportedFormats) }
        onPasted(text)
        emit(AddConfigEffect.OpenPreview)
    }

    /** The clipboard's text, read by the route because that is where a `Context` belongs. */
    fun onPasted(text: Secret<String>) {
        val result = parsers.parse(ConfigSource.PlainText(text))
        draft = (result as? ParseResult.Recognized)?.draft
        val detection = result.toDetection()
        _state.update { it.copy(detection = detection, name = detection.suggestedName()) }
    }


    private fun selectMethod(method: ImportMethodUi) {
        // No availability check any more: every value of `ImportMethodUi` is a way in that exists.
        // The check used to guard against a disabled row being tapped; there are no disabled rows.
        draft = null
        _state.update { AddConfigUiState(method = method, supportedFormats = it.supportedFormats) }
        emit(AddConfigEffect.OpenPreview)
    }

    private fun continueToReview() {
        if (_state.value.detection !is DetectionUiState.Recognized) return
        emit(AddConfigEffect.OpenReview)
    }

    /** Stores the draft under the name the user confirmed. */
    private fun save() {
        val draft = draft ?: return
        val state = _state.value
        if (!state.canSave) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val id = profiles.create(
                draft = draft,
                name = state.name.trim(),
                origin = ProfileOrigin.IMPORTED_LINK,
            )
            // A newly imported configuration becomes the selected one.
            selected.select(id)
            this@AddConfigViewModel.draft = null
            _state.update { it.copy(saving = false) }
            _effects.send(AddConfigEffect.OpenDone)
        }
    }

    private fun emit(effect: AddConfigEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }
}

internal fun DetectionUiState.suggestedName(): String =
    (this as? DetectionUiState.Recognized)?.preview?.suggestedName.orEmpty()
