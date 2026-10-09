package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VazieScreenPreview
@Composable
private fun AccountScreenPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        AccountScreen(
            state = AccountSectionUiState(
                account = AccountUi.SignedIn(
                    email = "person@example.com",
                    status = "ACTIVE",
                    plus = PlusAccess(validUntil = "2027-09-26T00:00:00Z"),
                ),
            ),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Offline · ru · large font", widthDp = 320, heightDp = 760, fontScale = 1.5f, locale = "ru")
@Composable
private fun AccountScreenOfflinePreview() {
    VazieTheme {
        AccountScreen(
            state = AccountSectionUiState(account = AccountUi.Offline(email = "person@example.com")),
            onAction = {},
            onBack = {},
        )
    }
}
