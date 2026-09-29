package app.vazie.vpn.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VaziePreview
import app.vazie.vpn.core.designsystem.preview.VaziePreviewTheme
import app.vazie.vpn.core.designsystem.theme.Appearance

@VaziePreview
@Composable
private fun VazieTechnicalSectionAppearancePreview(
    @PreviewParameter(Appearances::class) appearance: Appearance,
) {
    VaziePreviewTheme(appearance) { TechnicalRows() }
}

@Composable
private fun TechnicalRows() {
    VazieTechnicalSection(title = "Diagnostics") {
        VazieTechnicalRow(label = "engine", value = "xray")
        VazieTechnicalRow(label = "protocol", value = "vless · reality", highlighted = true)
        VazieTechnicalRow(label = "endpoint", value = "relay.example.net:443")
        VazieTechnicalRow(label = "latency", value = "32 ms")
    }
}
