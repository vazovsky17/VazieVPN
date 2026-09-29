package app.vazie.vpn.feature.account.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VaziePreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.account.AccountSectionUiState
import app.vazie.vpn.feature.account.AccountUi

@VaziePreview
@Composable
private fun AccountSettingsSectionSignedOutPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        AccountSettingsSection(state = AccountSectionUiState(account = AccountUi.SignedOut), onAction = {})
    }
}

@VaziePreview
@Composable
private fun AccountSettingsSectionSignedInPreview() {
    VazieTheme(appearance = Appearance.MILK) {
        AccountSettingsSection(
            state = AccountSectionUiState(
                account = AccountUi.SignedIn(email = "you@example.com", status = "ACTIVE"),
            ),
            onAction = {},
        )
    }
}

@VaziePreview
@Composable
private fun AccountSettingsSectionOfflinePreview() {
    VazieTheme(appearance = Appearance.MILK) {
        AccountSettingsSection(
            state = AccountSectionUiState(account = AccountUi.Offline(email = null)),
            onAction = {},
        )
    }
}
