package app.vazie.vpn.feature.account

import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusCatalog
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPrice
import app.vazie.vpn.account.api.PlusPurchase

/** A catalogue for previews only. The amounts are samples, deliberately not the prices on sale: the real ones are
 * the backend's and exist nowhere in this app. */
internal object PreviewPlusCatalog {

    val catalog = PlusCatalog(
        plans = listOf(
            PlusPlan("VPN_PLUS_MONTHLY", "VPN Plus — 1 month", PlusBillingPeriod.MONTHLY, 1, PlusPrice(19_900, "RUB"), false),
            PlusPlan("VPN_PLUS_YEARLY", "VPN Plus — 1 year", PlusBillingPeriod.YEARLY, 12, PlusPrice(199_000, "RUB"), false),
        ),
        purchase = PlusPurchase.PaymentPage,
    )

    val ready: PlusCatalogState = PlusCatalogState.Ready(catalog, stale = false)
}
