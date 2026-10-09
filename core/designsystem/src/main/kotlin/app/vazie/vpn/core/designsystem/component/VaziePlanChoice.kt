package app.vazie.vpn.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** One subscription plan as a selectable card; selection is shown by ring, check and radio semantics.
 * Group several inside a `selectableGroup()`. */
@Composable
fun VaziePlanChoice(
    period: String,
    price: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    badge: String? = null,
) {
    val colors = VazieTheme.colors
    val spacing = VazieTheme.spacing
    val shape = VazieTheme.shapes.lg
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = spacing.minTouchTarget)
            .clip(shape)
            .background(if (selected) colors.accentContainer else colors.surface)
            .border(
                width = if (selected) VazieTheme.borders.strong else VazieTheme.borders.regular,
                color = if (selected) colors.primary else colors.border,
                shape = shape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = spacing.md, vertical = spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xxs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                Text(
                    text = period,
                    style = VazieTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (badge != null) VazieBadge(label = badge, tone = VazieBadgeTone.Strong)
            }
            Text(
                text = price,
                style = VazieTheme.typography.headline,
                color = colors.textPrimary,
                maxLines = 1,
            )
            if (note != null) {
                Text(
                    text = note,
                    style = VazieTheme.typography.bodySecondary,
                    color = colors.textSecondary,
                )
            }
        }
        Icon(
            imageVector = VazieIcons.Check,
            contentDescription = null,
            tint = if (selected) colors.primary else colors.border,
            modifier = Modifier.size(spacing.lg),
        )
    }
}
