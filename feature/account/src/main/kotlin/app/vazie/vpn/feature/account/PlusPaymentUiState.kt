package app.vazie.vpn.feature.account

import androidx.compose.runtime.Immutable
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusPlan
import java.net.URI

/** Where a VPN Plus payment is, as the screen shows it. */
@Immutable
sealed interface PlusPaymentUiState {

    /** Opening the payment on the backend. */
    data object Opening : PlusPaymentUiState

    /** The payment page is open. [url] is single-use and signed, so it is never printed. */
    data class Paying(val url: String) : PlusPaymentUiState {
        override fun toString(): String = "Paying(url=<hidden>)"
    }

    /** The payment page said it went through. Not a confirmation: the backend grants VPN Plus when the
     * provider confirms it server-to-server. */
    data object Sent : PlusPaymentUiState

    /** Cancelled, declined or closed before the end. */
    data object NotCompleted : PlusPaymentUiState

    /** The backend could not open the payment. */
    data class Problem(val failure: AccountFailure) : PlusPaymentUiState

    /** VPN Plus is bought by writing to the author at [contactUrl] right now, not on a payment page. [email]
     * is the account's, to put in the message, when this device knows it. */
    data class ByContact(
        val plan: PlusPlan,
        val contactUrl: String,
        val contactLabel: String,
        val email: String?,
    ) : PlusPaymentUiState {
        override fun toString(): String = "ByContact(plan=$plan)"
    }
}

sealed interface PlusPaymentAction {
    data class PageFinished(val outcome: PaymentOutcome) : PlusPaymentAction
    data object Closed : PlusPaymentAction
    data object Retry : PlusPaymentAction
    data object Done : PlusPaymentAction
}

sealed interface PlusPaymentEffect {
    /** Nobody is signed in: sign in first, then come back to pay. */
    data object SignInFirst : PlusPaymentEffect
    data object Finished : PlusPaymentEffect
}

/** How the payment page ended, read off where it sent the browser. */
enum class PaymentOutcome { Sent, NotCompleted }

/** Robokassa's Success and Fail URLs are pages on the Vazie site ([siteHost]). When the payment page sends the browser to one
 * of them the payment is over, and the app takes over instead of loading the page. */
fun paymentOutcomeOf(url: String, siteHost: String): PaymentOutcome? {
    val uri = runCatching { URI(url) }.getOrNull() ?: return null
    if (uri.scheme != "https") return null
    val host = uri.host?.lowercase() ?: return null
    if (host != siteHost && host != "www.$siteHost") return null
    val path = uri.path.orEmpty().trimEnd('/')
    return when (path) {
        SUCCESS_PATH -> PaymentOutcome.Sent
        FAILED_PATH -> PaymentOutcome.NotCompleted
        else -> null
    }
}

private const val SUCCESS_PATH = "/payment/success"
private const val FAILED_PATH = "/payment/failed"
