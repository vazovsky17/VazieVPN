package app.vazie.vpn.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import app.vazie.vpn.core.designsystem.preview.VaziePreview
import app.vazie.vpn.core.designsystem.preview.VaziePreviewTheme
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** The two countries Vazie has artwork for, and one it does not. */
@VaziePreview
@Composable
private fun VazieCountryMarkPreview() {
    VaziePreviewTheme(Appearance.NIGHT_INDIGO) {
        Row(horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm)) {
            VazieCountryMark(countryCode = "NL")
            VazieCountryMark(countryCode = "DE")
            VazieCountryMark(countryCode = "CH")
        }
    }
    VaziePreviewTheme(Appearance.MILK) {
        Row(horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm)) {
            VazieCountryMark(countryCode = "NL")
            VazieCountryMark(countryCode = "DE")
            VazieCountryMark(countryCode = "CH")
        }
    }
}
