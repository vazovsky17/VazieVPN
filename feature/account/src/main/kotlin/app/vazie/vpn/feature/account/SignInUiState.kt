package app.vazie.vpn.feature.account

import androidx.compose.runtime.Immutable

/** The two steps of email sign-in: the address, then the code sent to it. One state for both, because the
 * second step is meaningless without the first's address and the countdown it started. */
@Immutable
data class SignInUiState(
    val step: SignInStep = SignInStep.EMAIL,
    val email: String = "",
    val code: String = "",
    val loading: Boolean = false,
    val problem: SignInProblem? = null,
    /** Seconds until "send again" is offered. `0` means it is offered now. */
    val resendSecondsLeft: Long = 0,
    /** A resend just went out, and the screen may say so. */
    val codeResent: Boolean = false,
) {
    /** The address as typed so far, judged. */
    val emailCheck: EmailCheck get() = EmailFormat.check(email)
    /** Whole addresses to offer while the domain is still being typed. */
    val emailCompletions: List<String> get() = EmailFormat.completions(email)
    val canRequestCode: Boolean get() = !loading && EmailFormat.isPlausible(email)
    val canVerify: Boolean get() = !loading && code.length == CODE_LENGTH
    val canResend: Boolean get() = !loading && resendSecondsLeft == 0L

    // The address and the code stay out of logs and crash reports even if someone prints the state.
    override fun toString(): String =
        "SignInUiState(step=$step, loading=$loading, problem=$problem, resendSecondsLeft=$resendSecondsLeft)"

    companion object {
        const val CODE_LENGTH: Int = 6
    }
}

enum class SignInStep { EMAIL, CODE }

/** What went wrong, in the terms the screen shows. */
sealed interface SignInProblem {
    data object InvalidEmail : SignInProblem
    data object InvalidCode : SignInProblem
    data class RateLimited(val retryAfterSeconds: Long?) : SignInProblem
    data object MailUnavailable : SignInProblem
    data object NotEnabled : SignInProblem
    data object Network : SignInProblem
    data object Server : SignInProblem
}

sealed interface SignInAction {
    data class EmailChanged(val email: String) : SignInAction
    data object RequestCode : SignInAction
    data class CodeChanged(val code: String) : SignInAction
    data object Verify : SignInAction
    data object Resend : SignInAction
    data object ChangeEmail : SignInAction
    data object Close : SignInAction
}

sealed interface SignInEffect {
    data object SignedIn : SignInEffect
    data object Close : SignInEffect
}
