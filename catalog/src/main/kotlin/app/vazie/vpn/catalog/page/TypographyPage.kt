package app.vazie.vpn.catalog.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieTechnicalRow
import app.vazie.vpn.core.designsystem.component.VazieTechnicalSection
import app.vazie.vpn.core.designsystem.theme.VazieFontFamilies
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.theme.DefaultTypefaces

// Both scripts and the digits in one line, so a face that handles Latin but not Cyrillic is obvious
// at a glance in either locale. Specimen data, not prose — it is not translated.
private const val Specimen = "Подключено · Connected · 0123456789"

@Composable
internal fun TypographyPage() {
    val t = VazieTheme.typography
    val faces = DefaultTypefaces
    val samples = listOf(
        Triple("display", t.display, R.string.catalog_type_display),
        Triple("headline", t.headline, R.string.catalog_type_headline),
        Triple("title", t.title, R.string.catalog_type_title),
        Triple("titleSmall", t.titleSmall, R.string.catalog_type_title_small),
        Triple("body", t.body, R.string.catalog_type_body),
        Triple("label", t.label, R.string.catalog_type_label),
        Triple("labelSmall", t.labelSmall, R.string.catalog_type_label_small),
        Triple("caption", t.caption, R.string.catalog_type_caption),
        Triple("mono", t.mono, R.string.catalog_type_mono),
        Triple("monoSmall", t.monoSmall, R.string.catalog_type_mono_small),
        Triple("monoMicro", t.monoMicro, R.string.catalog_type_mono_micro),
    )
    CatalogPage {
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_faces),
                note = stringResource(R.string.catalog_note_fonts),
            ) {
                SpecimenRow(stringResource(R.string.catalog_face_product)) {
                    FaceName(faces.product)
                }
                SpecimenRow(stringResource(R.string.catalog_face_label)) {
                    FaceName(faces.label)
                }
                SpecimenRow(stringResource(R.string.catalog_face_technical)) {
                    FaceName(faces.technical)
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_type_specimen)) {
                Text(Specimen, style = t.headline, color = VazieTheme.colors.textPrimary)
                Text(Specimen, style = t.body, color = VazieTheme.colors.textPrimary)
                Text(Specimen, style = t.mono, color = VazieTheme.colors.textPrimary)
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_type_buttons)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs)) {
                    VazieButton(stringResource(R.string.catalog_action_connect), onClick = {})
                    VazieButton(
                        text = stringResource(R.string.catalog_action_cancel),
                        onClick = {},
                        variant = VazieButtonVariant.Secondary,
                    )
                }
            }
        }
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_type_technical),
                note = stringResource(R.string.catalog_note_technical_type),
            ) {
                VazieTechnicalSection {
                    VazieTechnicalRow(label = "endpoint", value = "relay.example.net:443")
                    VazieTechnicalRow(label = "protocol", value = "vless · reality", highlighted = true)
                    VazieTechnicalRow(label = "latency", value = "32 ms")
                    VazieTechnicalRow(label = "rx / tx", value = "214 MB / 38 MB")
                }
            }
        }
        items(samples, key = { it.first }) { (name, style, sampleRes) ->
            TypeRow(name, style, stringResource(sampleRes))
        }
    }
}

@Composable
private fun FaceName(family: FontFamily) {
    val name = when (family) {
        VazieFontFamilies.geologica -> "Geologica"
        VazieFontFamilies.geologicaHeading -> "Geologica · SHRP 40"
        VazieFontFamilies.martianMono -> "Martian Mono"
        else -> "?"
    }
    Text(
        text = name,
        style = VazieTheme.typography.mono,
        color = VazieTheme.colors.primaryText,
    )
}

@Composable
private fun TypeRow(name: String, style: TextStyle, sample: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(VazieTheme.shapes.md)
            .background(VazieTheme.colors.surface)
            .padding(VazieTheme.spacing.sm),
    ) {
        Text(
            text = "$name · ${style.fontSize.value.toInt()}/${style.lineHeight.value.toInt()}",
            style = VazieTheme.typography.monoSmall,
            color = VazieTheme.colors.technicalLabel,
        )
        Text(sample, style = style, color = VazieTheme.colors.textPrimary)
    }
}
