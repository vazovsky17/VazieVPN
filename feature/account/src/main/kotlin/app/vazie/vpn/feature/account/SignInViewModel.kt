package app.vazie.vpn.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.core.model.Secret
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Email sign-in: ask for a code, then exchange it for a session. */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val account: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = _state.asStateFlow()

    private val _effects = Channel<SignInEffect>(Channel.BUFFERED)
    val effects: Flow<SignInEffect> = _effects.receiveAsFlow()

    private var countdown: Job? = null

    fun onAction(action: SignInAction) {
        when (action) {
            is SignInAction.EmailChanged ->
                _state.update { it.copy(email = action.email, problem = null) }
            SignInAction.RequestCode -> requestCode()
            is SignInAction.CodeChanged -> _state.update {
                it.copy(
                    code = action.code.filter(Char::isDigit).take(SignInUiState.CODE_LENGTH),
                    problem = null,
                )
            }
            SignInAction.Verify -> verify()
            SignInAction.Resend -> resend()
            SignInAction.ChangeEmail -> {
                countdown?.cancel()
                _state.update {
                    it.copy(step = SignInStep.EMAIL, code = "", problem = null, codeResent = false)
                }
            }
            SignInAction.Close -> emit(SignInEffect.Close)
        }
    }

    private fun requestCode() {
        val current = _state.value
        if (current.loading || current.email.isBlank()) return
        if (!EmailFormat.isPlausible(current.email)) {
            _state.update { it.copy(problem = SignInProblem.InvalidEmail) }
            return
        }
        _state.update { it.copy(loading = true, problem = null) }
        viewModelScope.launch {
            when (val result = account.requestEmailCode(current.email.trim())) {
                is AccountResult.Success -> {
                    _state.update {
                        it.copy(
                            step = SignInStep.CODE,
                            email = current.email.trim(),
                            code = "",
                            loading = false,
                            codeResent = false,
                        )
                    }
                    startCountdown(result.value.resendAfterSeconds)
                }
                is AccountResult.Failure -> _state.update {
                    it.copy(loading = false, problem = result.reason.toProblem())
                }
            }
        }
    }

    private fun resend() {
        val current = _state.value
        if (current.step != SignInStep.CODE || !current.canResend) return
        _state.update { it.copy(loading = true, problem = null, codeResent = false) }
        viewModelScope.launch {
            when (val result = account.requestEmailCode(current.email)) {
                is AccountResult.Success -> {
                    _state.update { it.copy(loading = false, code = "", codeResent = true) }
                    startCountdown(result.value.resendAfterSeconds)
                }
                is AccountResult.Failure -> {
                    val reason = result.reason
                    _state.update { it.copy(loading = false, problem = reason.toProblem()) }
                    // "Too early" carries its own wait: honour it rather than offering the button
                    // again.
                    if (reason is AccountFailure.RateLimited) {
                        reason.retryAfterSeconds?.let(::startCountdown)
                    }
                }
            }
        }
    }

    private fun verify() {
        val current = _state.value
        if (current.step != SignInStep.CODE || !current.canVerify) return
        _state.update { it.copy(loading = true, problem = null) }
        viewModelScope.launch {
            when (val result = account.verifyEmailCode(current.email, Secret.of(current.code))) {
                is AccountResult.Success -> {
                    countdown?.cancel()
                    _state.update { it.copy(loading = false, code = "") }
                    _effects.send(SignInEffect.SignedIn)
                }
                is AccountResult.Failure -> _state.update {
                    it.copy(loading = false, code = "", problem = result.reason.toProblem())
                }
            }
        }
    }

    private fun startCountdown(seconds: Long) {
        countdown?.cancel()
        _state.update { it.copy(resendSecondsLeft = seconds.coerceAtLeast(0)) }
        countdown = viewModelScope.launch {
            while (_state.value.resendSecondsLeft > 0) {
                delay(TICK_MILLIS)
                _state.update { it.copy(resendSecondsLeft = (it.resendSecondsLeft - 1).coerceAtLeast(0)) }
            }
        }
    }

    private fun emit(effect: SignInEffect) {
        viewModelScope.launch { _effects.send(effect) }
    }

    private fun AccountFailure.toProblem(): SignInProblem = when (this) {
        AccountFailure.InvalidEmail -> SignInProblem.InvalidEmail
        AccountFailure.InvalidCode -> SignInProblem.InvalidCode
        is AccountFailure.RateLimited -> SignInProblem.RateLimited(retryAfterSeconds)
        AccountFailure.EmailUnavailable -> SignInProblem.MailUnavailable
        AccountFailure.NotEnabled -> SignInProblem.NotEnabled
        AccountFailure.Unreachable -> SignInProblem.Network
        AccountFailure.SessionExpired, AccountFailure.Unknown -> SignInProblem.Server
    }

    private companion object {
        const val TICK_MILLIS = 1_000L
    }
}
