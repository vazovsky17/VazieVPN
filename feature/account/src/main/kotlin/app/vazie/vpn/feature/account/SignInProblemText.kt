package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import java.util.Locale

@Composable
internal fun signInProblemText(problem: SignInProblem): String = when (problem) {
    SignInProblem.InvalidEmail -> stringResource(R.string.account_error_invalid_email)
    SignInProblem.InvalidCode -> stringResource(R.string.account_error_invalid_code)
    is SignInProblem.RateLimited -> problem.retryAfterSeconds
        ?.takeIf { it > 0 }
        ?.let { stringResource(R.string.account_error_rate_limited, formatWait(it)) }
        ?: stringResource(R.string.account_error_rate_limited_later)
    SignInProblem.MailUnavailable -> stringResource(R.string.account_error_mail)
    SignInProblem.NotEnabled -> stringResource(R.string.account_error_not_enabled)
    SignInProblem.Network -> stringResource(R.string.account_error_network)
    SignInProblem.Server -> stringResource(R.string.account_error_server)
}

/** `0:42`, `12:05`. Minutes and seconds, the way a countdown is read. */
internal fun formatWait(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0)
    return String.format(Locale.ROOT, "%d:%02d", safe / SECONDS_PER_MINUTE, safe % SECONDS_PER_MINUTE)
}

private const val SECONDS_PER_MINUTE = 60
