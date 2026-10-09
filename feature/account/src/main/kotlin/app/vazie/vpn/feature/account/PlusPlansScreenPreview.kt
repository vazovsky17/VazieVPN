package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VazieScreenPreview
@Composable
private fun PlusPlansScreenPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        PlusPlansScreen(catalog = PreviewPlusCatalog.ready, onConnect = {}, onRetry = {}, onBack = {})
    }
}

@Preview(name = "ru · large font", widthDp = 320, heightDp = 760, fontScale = 1.5f, locale = "ru")
@Composable
private fun PlusPlansScreenRussianPreview() {
    VazieTheme {
        PlusPlansScreen(
            catalog = PlusCatalogState.Ready(PreviewPlusCatalog.catalog, stale = true),
            onConnect = {},
            onRetry = {},
            onBack = {},
        )
    }
}

@Preview(name = "ru · plans unavailable", widthDp = 360, heightDp = 760, locale = "ru")
@Composable
private fun PlusPlansScreenUnavailablePreview() {
    VazieTheme {
        PlusPlansScreen(catalog = PlusCatalogState.Unavailable(AccountFailure.Unreachable), onConnect = {}, onRetry = {}, onBack = {})
    }
}

@Preview(name = "ru · plans loading", widthDp = 360, heightDp = 760, locale = "ru")
@Composable
private fun PlusPlansScreenLoadingPreview() {
    VazieTheme {
        PlusPlansScreen(catalog = PlusCatalogState.Loading, onConnect = {}, onRetry = {}, onBack = {})
    }
}
