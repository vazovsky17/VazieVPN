package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VazieScreenPreview
@Composable
private fun DeleteAccountScreenPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        DeleteAccountScreen(
            state = AccountSectionUiState(account = AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE")),
            onAction = {},
            onBack = {},
        )
    }
}

@Preview(name = "Typed · ru · large font", widthDp = 320, heightDp = 760, fontScale = 1.5f, locale = "ru")
@Composable
private fun DeleteAccountScreenTypedPreview() {
    VazieTheme {
        DeleteAccountScreen(
            state = AccountSectionUiState(
                account = AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE"),
                deleteConfirmation = "person@example.com",
                deleteReady = true,
            ),
            onAction = {},
            onBack = {},
        )
    }
}
