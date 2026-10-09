package app.vazie.vpn.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.theme.vazieUppercase

/** No product screen draws this any more; kept for :catalog and `VazieColorsContrastTest`. */
@Composable
fun VazieTechnicalSection(
    modifier: Modifier = Modifier,
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = VazieTheme.colors
    Column(
        modifier = modifier
            .clip(VazieTheme.shapes.lg)
            .background(colors.technicalSurface)
            .padding(VazieTheme.spacing.cardPadding),
        verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xxs),
    ) {
        if (title != null) {
            Text(
                text = title.vazieUppercase(),
                style = VazieTheme.typography.monoSmall,
                color = colors.technicalLabel,
                modifier = Modifier.padding(bottom = VazieTheme.spacing.xxs),
            )
        }
        content()
    }
}

/** One read-out. The label and value are joined for screen readers so they are announced as a pair rather
 * than as two disconnected fragments. */
@Composable
fun VazieTechnicalRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
) {
    val colors = VazieTheme.colors
    Column(modifier = modifier.semantics(mergeDescendants = true) {
        contentDescription = "$label: $value"
    }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = VazieTheme.spacing.xxs)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm),
        ) {
            Text(
                text = label.vazieUppercase(),
                style = VazieTheme.typography.monoSmall,
                color = colors.technicalLabel,
                modifier = Modifier.weight(VazieConsoleDefaults.LabelWeight),
            )
            Text(
                text = value,
                // The value is a size larger than its key, as in `VazieReadout`.
                style = VazieTheme.typography.mono,
                color = if (highlighted) colors.technicalValueAccent else colors.technicalValue,
                modifier = Modifier.weight(VazieConsoleDefaults.ValueWeight),
            )
        }
        HorizontalDivider(
            thickness = VazieTheme.borders.hairline,
            color = colors.technicalDivider,
        )
    }
}
