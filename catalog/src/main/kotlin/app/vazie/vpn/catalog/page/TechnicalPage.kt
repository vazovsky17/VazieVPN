package app.vazie.vpn.catalog.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.component.VazieTechnicalRow
import app.vazie.vpn.core.designsystem.component.VazieTechnicalSection

@Composable
internal fun TechnicalPage() {
    CatalogPage {
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_technical),
                note = stringResource(R.string.catalog_note_technical),
            ) {
                VazieTechnicalSection(title = stringResource(R.string.catalog_technical_title)) {
                    VazieTechnicalRow("engine", "xray")
                    VazieTechnicalRow("protocol", "vless · reality", highlighted = true)
                    VazieTechnicalRow("transport", "tcp · xtls-vision")
                    VazieTechnicalRow("endpoint", "relay.example.net:443")
                    VazieTechnicalRow("latency", "32 ms")
                    VazieTechnicalRow("rx / tx", "214 MB / 38 MB")
                }
            }
        }
    }
}
