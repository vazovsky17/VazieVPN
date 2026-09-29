package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VaziePlanChoice
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** VPN Plus: what it is, the two plans, and one action. The year is chosen by default because it is the
 * better value; either is one tap away. */
@Composable
fun PlusPlansScreen(
    onConnect: (PlusPlan) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    var plan by rememberSaveable { mutableStateOf(PlusPlan.YEARLY) }
    var consent by rememberSaveable { mutableStateOf(false) }
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.plus_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.account_back),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = spacing.screenHorizontal)
                .vazieScrollEdgePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            Text(
                text = stringResource(R.string.plus_body),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            Text(
                text = stringResource(R.string.plus_servers_growth),
                style = VazieTheme.typography.caption,
                color = VazieTheme.colors.textSecondary,
            )
            PlusPlanChoices(selected = plan, onSelect = { plan = it })
            // No purchase without the offer accepted.
            OfferConsent(checked = consent, onCheckedChange = { consent = it })
            VazieButton(
                text = stringResource(R.string.plus_connect),
                onClick = { onConnect(plan) },
                enabled = consent,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = stringResource(R.string.plus_terms),
                style = VazieTheme.typography.caption,
                color = VazieTheme.colors.textSecondary,
            )
        }
    }
}

/** The two plans as one choice. */
@Composable
internal fun PlusPlanChoices(selected: PlusPlan, onSelect: (PlusPlan) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
    ) {
        VaziePlanChoice(
            period = stringResource(R.string.plus_plan_month),
            price = stringResource(R.string.plus_price_month),
            selected = selected == PlusPlan.MONTHLY,
            onSelect = { onSelect(PlusPlan.MONTHLY) },
        )
        VaziePlanChoice(
            period = stringResource(R.string.plus_plan_year),
            price = stringResource(R.string.plus_price_year),
            note = stringResource(R.string.plus_plan_year_note),
            badge = stringResource(R.string.plus_plan_year_badge),
            selected = selected == PlusPlan.YEARLY,
            onSelect = { onSelect(PlusPlan.YEARLY) },
        )
    }
}
