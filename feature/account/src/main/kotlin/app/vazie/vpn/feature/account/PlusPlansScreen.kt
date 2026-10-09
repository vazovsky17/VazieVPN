package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** VPN Plus: what it is, the plans the backend sells, and one action. The year is chosen by default because it is
 * the better value; either is one tap away. Nothing is bought without a catalogue: while the plans load, or when
 * they cannot be read and none are stored, the button waits. */
@Composable
fun PlusPlansScreen(
    catalog: PlusCatalogState,
    onConnect: (planCode: String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    val (plan, onSelect) = rememberPlanChoice(catalog)
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
            PlusPlanChoices(state = catalog, selected = plan, onSelect = onSelect, onRetry = onRetry)
            // No purchase without the offer accepted.
            OfferConsent(checked = consent, onCheckedChange = { consent = it })
            VazieButton(
                text = stringResource(R.string.plus_connect),
                onClick = { plan?.let { onConnect(it.code) } },
                enabled = consent && plan != null,
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
