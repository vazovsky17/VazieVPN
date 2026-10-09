package app.vazie.vpn.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import app.vazie.vpn.core.designsystem.icon.VazieCountryArt
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** A country, drawn as its flag where Vazie has the artwork and as its code where it does not. */
@Composable
fun VazieCountryMark(
    countryCode: String,
    modifier: Modifier = Modifier,
) {
    val flag = VazieCountryArt.flag(countryCode)
    VazieMark(tone = VazieBadgeTone.Muted, modifier = modifier) {
        if (flag != null) {
            Image(
                painter = painterResource(flag),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .size(VazieTheme.spacing.lg)
                    .clearAndSetSemantics { },
            )
        } else {
            Text(
                text = countryCode.uppercase(),
                style = VazieTheme.typography.monoSmall,
                color = VazieTheme.colors.engineBadgeFg,
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
