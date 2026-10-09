package app.vazie.vpn.feature.onboarding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusCatalog
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPrice
import app.vazie.vpn.account.api.PlusPurchase
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Sample amounts, deliberately not the prices on sale: those are the backend's alone. */
private val previewCatalog = PlusCatalog(
    plans = listOf(
        PlusPlan("VPN_PLUS_MONTHLY", "VPN Plus — 1 month", PlusBillingPeriod.MONTHLY, 1, PlusPrice(19_900, "RUB"), false),
        PlusPlan("VPN_PLUS_YEARLY", "VPN Plus — 1 year", PlusBillingPeriod.YEARLY, 12, PlusPrice(199_000, "RUB"), false),
    ),
    purchase = PlusPurchase.PaymentPage,
)

@VazieScreenPreview
@Composable
private fun OnboardingPlusContentPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        OnboardingPlusContent(
            plans = PlusCatalogState.Ready(previewCatalog, stale = false),
            selected = previewCatalog.defaultPlan,
            onSelect = {},
            onRetry = {},
            consent = true,
            onConsentChange = {},
        )
    }
}

@Preview(name = "ru · large font", widthDp = 320, heightDp = 760, fontScale = 1.5f, locale = "ru")
@Composable
private fun OnboardingPlusContentRussianPreview() {
    VazieTheme {
        OnboardingPlusContent(
            plans = PlusCatalogState.Ready(previewCatalog, stale = true),
            selected = previewCatalog.plans.first(),
            onSelect = {},
            onRetry = {},
            consent = false,
            onConsentChange = {},
        )
    }
}

@Preview(name = "ru · unavailable", widthDp = 360, heightDp = 760, locale = "ru")
@Composable
private fun OnboardingPlusContentUnavailablePreview() {
    VazieTheme {
        OnboardingPlusContent(
            plans = PlusCatalogState.Unavailable(AccountFailure.Unreachable),
            selected = null,
            onSelect = {},
            onRetry = {},
            consent = false,
            onConsentChange = {},
        )
    }
}
