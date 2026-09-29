package app.vazie.vpn.feature.onboarding.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VazieScreenPreview
@Composable
private fun OnboardingPlusContentPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        OnboardingPlusContent(yearly = true, onSelect = {}, consent = true, onConsentChange = {})
    }
}

@Preview(name = "ru · large font", widthDp = 320, heightDp = 760, fontScale = 1.5f, locale = "ru")
@Composable
private fun OnboardingPlusContentRussianPreview() {
    VazieTheme {
        OnboardingPlusContent(yearly = false, onSelect = {}, consent = false, onConsentChange = {})
    }
}
