package app.vazie.vpn.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** The VPN Plus mark; "VPN+" only when [compact]. TalkBack always hears "VPN Plus". Not translated. */
@Composable
fun VaziePlusBadge(
    modifier: Modifier = Modifier,
    compact: Boolean = true,
) {
    val colors = VazieTheme.colors
    Row(
        modifier = modifier
            .heightIn(min = BadgeHeight)
            .clip(RoundedCornerShape(BadgeRadius))
            .background(Brush.horizontalGradient(listOf(colors.routeSoft, colors.surfaceMuted)))
            .clearAndSetSemantics { contentDescription = FullName }
            .padding(horizontal = BadgePaddingHorizontal),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val style = VazieTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
        Text(text = Product, style = style, color = colors.onAccentContainer)
        Text(text = if (compact) CompactSuffix else FullSuffix, style = style, color = colors.routeEnd)
    }
}

private const val Product = "VPN"
private const val CompactSuffix = "+"
private const val FullSuffix = " Plus"
private const val FullName = "VPN Plus"
private val BadgeHeight = 26.dp
private val BadgeRadius = 9.dp
private val BadgePaddingHorizontal = 10.dp
