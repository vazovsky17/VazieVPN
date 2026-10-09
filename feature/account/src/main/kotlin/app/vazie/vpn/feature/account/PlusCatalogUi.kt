package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusCatalog
import app.vazie.vpn.account.api.PlusCatalogRepository
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPrice
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VaziePlanChoice
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The VPN Plus plans for a screen that sells them: the backend's catalogue, read again each time such a screen
 * opens. */
@HiltViewModel
class PlusCatalogViewModel @Inject constructor(
    private val catalog: PlusCatalogRepository,
) : ViewModel() {

    val state: StateFlow<PlusCatalogState> = catalog.state

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch { catalog.refresh() }
    }
}

/** The plan code a screen has chosen, remembered across rotation: the person's pick while the catalogue still
 * sells it, otherwise the catalogue's default. `null` while there is no catalogue to choose from. */
@Composable
internal fun rememberPlanChoice(state: PlusCatalogState): Pair<PlusPlan?, (String) -> Unit> {
    var picked by rememberSaveable { mutableStateOf<String?>(null) }
    val catalog = (state as? PlusCatalogState.Ready)?.catalog
    val plan = catalog?.let { it.plan(picked) ?: it.defaultPlan }
    return plan to { code: String -> picked = code }
}

/** The plans as one choice, or what stands in for them: a wait while they load, and a way to try again when they
 * cannot be read and none are stored. A stale catalogue is shown with a word that it may have changed. */
@Composable
internal fun PlusPlanChoices(
    state: PlusCatalogState,
    selected: PlusPlan?,
    onSelect: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    when (state) {
        PlusCatalogState.Loading -> Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = spacing.lg),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                CircularProgressIndicator(color = VazieTheme.colors.primary)
                Text(
                    text = stringResource(R.string.plus_plans_loading),
                    style = VazieTheme.typography.body,
                    color = VazieTheme.colors.textSecondary,
                )
            }
        }
        is PlusCatalogState.Unavailable -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
            Text(
                text = stringResource(
                    if (state.failure == AccountFailure.Unreachable) R.string.plus_plans_unavailable_network else R.string.plus_plans_unavailable,
                ),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            VazieButton(
                text = stringResource(R.string.account_retry),
                onClick = onRetry,
                variant = VazieButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        is PlusCatalogState.Ready -> Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                state.catalog.plans.forEach { plan ->
                    PlanChoice(catalog = state.catalog, plan = plan, selected = plan == selected, onSelect = { onSelect(plan.code) })
                }
            }
            if (state.stale) {
                Text(
                    text = stringResource(R.string.plus_plans_stale),
                    style = VazieTheme.typography.caption,
                    color = VazieTheme.colors.textSecondary,
                )
                VazieButton(
                    text = stringResource(R.string.plus_plans_refresh),
                    onClick = onRetry,
                    variant = VazieButtonVariant.Text,
                )
            }
        }
    }
}

@Composable
private fun PlanChoice(catalog: PlusCatalog, plan: PlusPlan, selected: Boolean, onSelect: () -> Unit) {
    val betterValue = catalog.isBetterValue(plan)
    VaziePlanChoice(
        period = plan.periodLabel(),
        price = plan.price.display(),
        note = plan.takeIf { betterValue }?.price?.perMonth(plan.months)?.let { stringResource(R.string.plus_plan_per_month, it.display()) },
        badge = if (betterValue) stringResource(R.string.plus_plan_year_badge) else null,
        selected = selected,
        onSelect = onSelect,
    )
}

/** "1 месяц", "1 год": the period the plan is sold for. */
@Composable
internal fun PlusPlan.periodLabel(): String = stringResource(
    when (billingPeriod) {
        PlusBillingPeriod.MONTHLY -> R.string.plus_plan_month
        PlusBillingPeriod.YEARLY -> R.string.plus_plan_year
    },
)

/** The price in the person's own locale: `299 ₽`. */
@Composable
internal fun PlusPrice.display(): String = format(LocalConfiguration.current.locales[0])
