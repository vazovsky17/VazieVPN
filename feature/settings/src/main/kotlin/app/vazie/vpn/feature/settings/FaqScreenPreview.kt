package app.vazie.vpn.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VazieScreenPreview
@Composable
private fun FaqScreenPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        FaqScreen(onBack = {})
    }
}

@Preview(name = "ru · large font", widthDp = 320, heightDp = 900, fontScale = 1.5f, locale = "ru")
@Composable
private fun FaqScreenRussianPreview() {
    VazieTheme {
        FaqScreen(onBack = {})
    }
}
