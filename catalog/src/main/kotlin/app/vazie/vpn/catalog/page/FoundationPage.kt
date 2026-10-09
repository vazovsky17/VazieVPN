package app.vazie.vpn.catalog.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.theme.vazieShadow

@Composable
internal fun FoundationPage() {
    val s = VazieTheme.spacing
    CatalogPage {
        item {
            Specimen(stringResource(R.string.catalog_group_spacing)) {
                listOf(
                    "xxs" to s.xxs, "xs" to s.xs, "sm" to s.sm, "md" to s.md,
                    "lg" to s.lg, "xl" to s.xl, "xxl" to s.xxl, "xxxl" to s.xxxl,
                ).forEach { (name, value) -> ScaleRow(name, value) }
            }
        }
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_spacing_aliases),
                note = stringResource(R.string.catalog_note_density),
            ) {
                listOf(
                    "screenHorizontal" to s.screenHorizontal,
                    "cardPadding" to s.cardPadding,
                    "listRowHeight" to s.listRowHeight,
                    "sectionGap" to s.sectionGap,
                    "minTouchTarget" to s.minTouchTarget,
                ).forEach { (name, value) -> SpecimenRow(name) { ValueText("${value.value.toInt()}dp") } }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_shapes)) {
                val shapes = VazieTheme.shapes
                ShapeRow("sm · ${shapes.smRadius.value.toInt()}dp", shapes.sm)
                ShapeRow("md · ${shapes.mdRadius.value.toInt()}dp", shapes.md)
                ShapeRow("lg · ${shapes.lgRadius.value.toInt()}dp", shapes.lg)
                ShapeRow("xl · ${shapes.xlRadius.value.toInt()}dp", shapes.xl)
                ShapeRow("pill", shapes.pill)
                ShapeRow("sheet", shapes.sheet)
            }
        }
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_borders),
                note = stringResource(R.string.catalog_note_borders),
            ) {
                val b = VazieTheme.borders
                listOf("hairline" to b.hairline, "regular" to b.regular, "strong" to b.strong)
                    .forEach { (name, value) ->
                        SpecimenRow(name) {
                            Box(
                                Modifier
                                    .width(80.dp)
                                    .height(value)
                                    .background(VazieTheme.colors.textPrimary),
                            )
                        }
                    }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_elevation)) {
                Row(horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.lg)) {
                    listOf(
                        "resting" to VazieTheme.elevation.restingAlpha,
                        "floating" to VazieTheme.elevation.floatingAlpha,
                    ).forEach { (name, alpha) ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                Modifier
                                    .size(88.dp)
                                    .vazieShadow(VazieTheme.shapes.lg, alpha = alpha)
                                    .clip(VazieTheme.shapes.lg)
                                    .background(VazieTheme.colors.elevated),
                            )
                            StateLabel(name, Modifier.padding(top = VazieTheme.spacing.xxs))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScaleRow(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StateLabel(name, Modifier.width(64.dp))
        Box(
            Modifier
                .height(20.dp)
                .width(value)
                .clip(VazieTheme.shapes.sm)
                .background(VazieTheme.colors.primary),
        )
        ValueText(
            text = "${value.value.toInt()}dp",
            modifier = Modifier.padding(start = VazieTheme.spacing.xs),
        )
    }
}

@Composable
private fun ShapeRow(name: String, shape: Shape) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .vazieShadow(shape, alpha = VazieTheme.elevation.floatingAlpha)
                .clip(shape)
                .background(VazieTheme.colors.surfaceMuted),
        )
        StateLabel(name, Modifier.padding(start = VazieTheme.spacing.sm))
    }
}

@Composable
private fun ValueText(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = VazieTheme.typography.mono,
        color = VazieTheme.colors.primaryText,
        modifier = modifier,
    )
}
