package app.vazie.vpn.catalog.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.theme.VazieColors
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

private data class Swatch(
    val name: String,
    val color: Color,
    val onColor: Color?,
    val group: Int,
    /** Disabled text is exempt from WCAG AA, so its ratio is shown but never marked as a failure — a badge
     * that is always red is a badge everyone learns to ignore. */
    val contrastRequired: Boolean = true,
)

@Composable
internal fun ColorsPage() {
    val c = VazieTheme.colors
    val swatches = remember(c) { swatches(c) }
    CatalogPage {
        swatches.groupBy { it.group }.forEach { (groupRes, entries) ->
            item(key = "h$groupRes") {
                Text(
                    text = stringResource(groupRes),
                    style = VazieTheme.typography.label,
                    color = VazieTheme.colors.textSecondary,
                    modifier = Modifier.padding(top = VazieTheme.spacing.xs),
                )
            }
            items(entries, key = { it.name }) { SwatchRow(it) }
        }
        item {
            Text(
                text = stringResource(R.string.catalog_note_contrast),
                style = VazieTheme.typography.caption,
                color = VazieTheme.colors.textSecondary,
                modifier = Modifier.padding(top = VazieTheme.spacing.md),
            )
        }
    }
}

private fun swatches(c: VazieColors) = listOf(
    Swatch("background", c.background, c.textPrimary, R.string.catalog_group_surfaces),
    Swatch("surface", c.surface, c.textPrimary, R.string.catalog_group_surfaces),
    Swatch("elevated", c.elevated, c.textPrimary, R.string.catalog_group_surfaces),
    Swatch("border", c.border, null, R.string.catalog_group_surfaces),
    Swatch("disabled", c.disabled, c.textDisabled, R.string.catalog_group_surfaces, contrastRequired = false),
    Swatch("scrim", c.scrim, null, R.string.catalog_group_surfaces),
    Swatch("inverseSurface", c.inverseSurface, c.onInverseSurface, R.string.catalog_group_surfaces),
    Swatch("onInverseSurface", c.onInverseSurface, c.inverseSurface, R.string.catalog_group_surfaces),
    Swatch("inverseAccent", c.inverseAccent, c.inverseSurface, R.string.catalog_group_surfaces),

    Swatch("primary", c.primary, c.onPrimary, R.string.catalog_group_brand),
    Swatch("onPrimary", c.onPrimary, c.primary, R.string.catalog_group_brand),
    Swatch("secondary", c.secondary, null, R.string.catalog_group_brand),
    Swatch("accent", c.accent, null, R.string.catalog_group_brand),
    Swatch("primaryPressed", c.primaryPressed, null, R.string.catalog_group_brand),

    Swatch("textPrimary", c.textPrimary, c.background, R.string.catalog_group_text),
    Swatch("textSecondary", c.textSecondary, c.background, R.string.catalog_group_text),
    Swatch("textDisabled", c.textDisabled, c.background, R.string.catalog_group_text),
    Swatch("primaryText", c.primaryText, c.background, R.string.catalog_group_text_accessible),
    Swatch("successText", c.successText, c.surface, R.string.catalog_group_text_accessible),
    Swatch("warningText", c.warningText, c.surface, R.string.catalog_group_text_accessible),
    Swatch("errorText", c.errorText, c.surface, R.string.catalog_group_text_accessible),

    Swatch("success", c.success, null, R.string.catalog_group_status_fills),
    Swatch("warning", c.warning, null, R.string.catalog_group_status_fills),
    Swatch("error", c.error, c.elevated, R.string.catalog_group_status_fills),
    Swatch("statusDotIdle", c.statusDotIdle, null, R.string.catalog_group_status_fills),

    Swatch("surfaceMuted", c.surfaceMuted, c.onSurfaceMuted, R.string.catalog_group_containers),
    Swatch("onSurfaceMuted", c.onSurfaceMuted, c.surfaceMuted, R.string.catalog_group_containers),
    Swatch("engineBadgeFg", c.engineBadgeFg, c.surfaceMuted, R.string.catalog_group_containers),
    Swatch("accentContainer", c.accentContainer, c.onAccentContainer, R.string.catalog_group_containers),
    Swatch("onAccentContainer", c.onAccentContainer, c.accentContainer, R.string.catalog_group_containers),
    Swatch("errorContainer", c.errorContainer, c.onErrorContainer, R.string.catalog_group_containers),
    Swatch("onErrorContainer", c.onErrorContainer, c.errorContainer, R.string.catalog_group_containers),
    Swatch("warningContainer", c.warningContainer, c.onWarningContainer, R.string.catalog_group_containers),
    Swatch("onWarningContainer", c.onWarningContainer, c.warningContainer, R.string.catalog_group_containers),
    Swatch("dividerSubtle", c.dividerSubtle, null, R.string.catalog_group_containers),
    Swatch("segmentedTrack", c.segmentedTrack, null, R.string.catalog_group_containers),
    Swatch("fieldDisabledBg", c.fieldDisabledBg, null, R.string.catalog_group_containers),

    Swatch("technicalSurface", c.technicalSurface, c.technicalValue, R.string.catalog_group_technical_tokens),
    Swatch("technicalSurfaceAlt", c.technicalSurfaceAlt, c.technicalValue, R.string.catalog_group_technical_tokens),
    Swatch("technicalLabel", c.technicalLabel, c.technicalSurface, R.string.catalog_group_technical_tokens),
    Swatch("technicalValue", c.technicalValue, c.technicalSurface, R.string.catalog_group_technical_tokens),
    Swatch("technicalValueAccent", c.technicalValueAccent, c.technicalSurface, R.string.catalog_group_technical_tokens),
    Swatch("technicalDivider", c.technicalDivider, null, R.string.catalog_group_technical_tokens),
    Swatch("technicalBadgeBg", c.technicalBadgeBg, null, R.string.catalog_group_technical_tokens),
)

@Composable
private fun SwatchRow(swatch: Swatch) {
    val colors = VazieTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(VazieTheme.shapes.md)
            .background(colors.surface)
            .padding(VazieTheme.spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(VazieTheme.shapes.sm)
                .background(swatch.color)
                .border(1.dp, colors.border, VazieTheme.shapes.sm),
            contentAlignment = Alignment.Center,
        ) {
            if (swatch.onColor != null) {
                Text("Aa", style = VazieTheme.typography.labelSmall, color = swatch.onColor)
            }
        }
        Column(
            Modifier
                .weight(1f)
                .padding(start = VazieTheme.spacing.sm),
        ) {
            Text(swatch.name, style = VazieTheme.typography.titleSmall, color = colors.textPrimary)
            Text(swatch.color.hex(), style = VazieTheme.typography.monoSmall, color = colors.textSecondary)
        }
        if (swatch.onColor != null) {
            val ratio = contrast(swatch.color, swatch.onColor)
            val passes = ratio >= MinContrast || !swatch.contrastRequired
            Text(
                // Ratios are measurements, not prose: the same decimal separator in every locale.
                text = String.format(Locale.ROOT, "%.2f", ratio),
                style = VazieTheme.typography.monoSmall,
                color = if (passes) colors.onSurfaceMuted else colors.onErrorContainer,
                modifier = Modifier
                    .clip(VazieTheme.shapes.pill)
                    .background(if (passes) colors.surfaceMuted else colors.errorContainer)
                    .padding(horizontal = VazieTheme.spacing.xs, vertical = 2.dp),
            )
        }
    }
}

private const val MinContrast = 4.5

private fun Color.hex() = String.format(
    Locale.ROOT,
    "#%02X%02X%02X",
    (red * 255).toInt().coerceIn(0, 255),
    (green * 255).toInt().coerceIn(0, 255),
    (blue * 255).toInt().coerceIn(0, 255),
)

private fun Color.luminance(): Double {
    fun ch(v: Float): Double {
        val d = v.toDouble()
        return if (d <= 0.03928) d / 12.92 else ((d + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * ch(red) + 0.7152 * ch(green) + 0.0722 * ch(blue)
}

private fun contrast(a: Color, b: Color): Double {
    val la = a.luminance()
    val lb = b.luminance()
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}
