package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusSubscription
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.VazieTheme

private class ManageStates : PreviewParameterProvider<PlusManageUiState> {
    override val values = sequenceOf(
        PlusManageUiState.Loading,
        PlusManageUiState.Loaded(PlusSubscription("VPN_PLUS_YEARLY", "VPN Plus — 1 год", "active", "2027-09-26T00:00:00Z")),
        PlusManageUiState.Problem(AccountFailure.Unreachable),
    )
}

@VazieScreenPreview
@Composable
private fun PlusManageScreenPreview(@PreviewParameter(ManageStates::class) state: PlusManageUiState) {
    VazieTheme {
        PlusManageScreen(state = state, catalog = PreviewPlusCatalog.ready, onExtend = {}, onRetry = {}, onRetryCatalog = {}, onBack = {})
    }
}

@Preview(name = "ru · large font", widthDp = 320, heightDp = 900, fontScale = 1.5f, locale = "ru")
@Composable
private fun PlusManageScreenRussianPreview() {
    VazieTheme {
        PlusManageScreen(
            state = PlusManageUiState.Loaded(PlusSubscription("VPN_PLUS_MONTHLY", "VPN Plus — 1 месяц", "active", "2026-10-26T00:00:00Z")),
            catalog = PreviewPlusCatalog.ready,
            onExtend = {},
            onRetry = {},
            onRetryCatalog = {},
            onBack = {},
        )
    }
}
