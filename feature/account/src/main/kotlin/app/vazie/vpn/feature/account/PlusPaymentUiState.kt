package app.vazie.vpn.feature.account

import androidx.compose.runtime.Immutable

/** Where paying for VPN Plus is, as the screen shows it. The payment itself happens on the site. */
@Immutable
sealed interface PlusPaymentUiState {

    /** Making sure somebody is signed in, then opening the site. */
    data object Opening : PlusPaymentUiState

    /** The site is open. [checking] — the backend is being asked whether VPN Plus is on; [notSeen] — it was asked
     * and VPN Plus is not on yet. */
    data class Awaiting(val checking: Boolean = false, val notSeen: Boolean = false) : PlusPaymentUiState

    /** VPN Plus is on for the account. */
    data object Activated : PlusPaymentUiState

    /** No browser took the link to the site. */
    data object SiteNotOpened : PlusPaymentUiState
}

sealed interface PlusPaymentAction {
    /** The screen is in front again: back from the site, or opened anew. */
    data object Resumed : PlusPaymentAction

    /** The person asked to check now. */
    data object Check : PlusPaymentAction

    /** The person asked for the site again. */
    data object OpenSiteAgain : PlusPaymentAction

    /** The link to the site went to a browser. */
    data object SiteOpened : PlusPaymentAction

    /** No browser took the link. */
    data object SiteNotOpened : PlusPaymentAction

    data object Done : PlusPaymentAction
}

sealed interface PlusPaymentEffect {
    /** Nobody is signed in: sign in first, then come back to pay. */
    data object SignInFirst : PlusPaymentEffect

    /** Open the site on [planCode] in the browser. */
    data class OpenSite(val planCode: String) : PlusPaymentEffect

    data object Finished : PlusPaymentEffect
}
