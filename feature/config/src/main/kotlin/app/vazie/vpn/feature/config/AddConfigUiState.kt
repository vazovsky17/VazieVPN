package app.vazie.vpn.feature.config

import androidx.compose.runtime.Immutable
import app.vazie.vpn.config.InvalidReason
import app.vazie.vpn.config.UnsupportedFeature
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/** The in-flight import. */
@Immutable
data class AddConfigUiState(
    val method: ImportMethodUi? = null,
    val detection: DetectionUiState = DetectionUiState.AwaitingInput,
    val name: String = "",
    val saving: Boolean = false,
    /** The formats Vazie can read, as one already-formatted phrase. */
    val supportedFormats: String = "",
) {
    val canSave: Boolean
        get() = detection is DetectionUiState.Recognized && name.isNotBlank() && !saving
}

/** How a configuration comes in. */
enum class ImportMethodUi {
    PASTE,
}

/** What Vazie made of what the user gave it. */
@Immutable
sealed interface DetectionUiState {

    /** Nothing has been given to Vazie yet. */
    data object AwaitingInput : DetectionUiState

    data class Recognized(val preview: ConfigPreviewUi) : DetectionUiState

    /** Read correctly, and Vazie has no support for part of it — its own outcome, not an error. */
    data class Unsupported(
        val protocolLabel: String,
        val feature: UnsupportedFeature,
    ) : DetectionUiState

    data class Invalid(val reason: InvalidReason) : DetectionUiState
}

/** Safe fields only. There is no field here that could hold a user id, a key or a short id, so the preview
 * card cannot draw one however it is written — the type is the guarantee. */
@Immutable
data class ConfigPreviewUi(
    val protocolLabel: String,
    val securityLabel: String?,
    val transportLabel: String?,
    val server: String,
    val endpointHost: String,
    val endpointPort: Int,
    val serverName: String?,
    val fingerprint: String?,
    val flow: String?,
    val suggestedName: String,
    val mark: String,
    val keptParameters: ImmutableList<String> = persistentListOf(),
)

sealed interface AddConfigAction {
    data class SelectMethod(val method: ImportMethodUi) : AddConfigAction

    /** The user asked for the clipboard to be read. The screen never touches the clipboard itself — see
     * `AddConfigRoutes`. */
    data object PasteRequested : AddConfigAction
    data object Continue : AddConfigAction
    data class NameChanged(val name: String) : AddConfigAction
    data object Save : AddConfigAction
    data object Close : AddConfigAction
}

sealed interface AddConfigEffect {
    data object OpenPreview : AddConfigEffect
    data object OpenReview : AddConfigEffect
    data object OpenDone : AddConfigEffect
    data object Close : AddConfigEffect

    /** Read the clipboard and hand the text back through `AddConfigViewModel.onPasted`. */
    data object ReadClipboard : AddConfigEffect
}
