package app.vazie.vpn.core.designsystem.component

import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VaziePreview
import app.vazie.vpn.core.designsystem.preview.VaziePreviewTheme
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VaziePreview
@Composable
private fun VaziePlanChoicePreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VaziePreviewTheme(appearance) {
        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs)) {
            VaziePlanChoice(period = "1 month", price = "299 ₽", selected = false, onSelect = {})
            VaziePlanChoice(
                period = "1 year",
                price = "2 990 ₽",
                note = "About 249 ₽ a month",
                badge = "Better value",
                selected = true,
                onSelect = {},
            )
        }
    }
}

@Preview(name = "Narrow · large font", widthDp = 280, fontScale = 1.6f, locale = "ru")
@Composable
private fun VaziePlanChoiceNarrowPreview() {
    VaziePreviewTheme {
        VaziePlanChoice(
            period = "1 год",
            price = "2 990 ₽",
            note = "Около 249 ₽ за месяц",
            badge = "Выгоднее",
            selected = true,
            onSelect = {},
        )
    }
}
