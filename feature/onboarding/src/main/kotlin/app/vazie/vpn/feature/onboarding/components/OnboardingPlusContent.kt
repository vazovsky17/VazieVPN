package app.vazie.vpn.feature.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieConsentCheck
import app.vazie.vpn.core.designsystem.component.VaziePlanChoice
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.onboarding.R

/** The last onboarding page: VPN Plus and the plans the backend sells, at its prices. The page chooses a plan; the
 * buttons under the pager act on it. While the plans load there is a wait, and when they cannot be read and none
 * are stored, a way to try again — never a price this app made up. */
@Composable
internal fun OnboardingPlusContent(
    plans: PlusCatalogState,
    selected: PlusPlan?,
    onSelect: (planCode: String) -> Unit,
    onRetry: () -> Unit,
    consent: Boolean,
    onConsentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md)
            .vazieScrollEdgePadding(extra = spacing.zero),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(R.string.onboarding_plus_title),
            style = VazieTheme.typography.display,
            color = VazieTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.onboarding_plus_body),
            style = VazieTheme.typography.body,
            color = VazieTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm, bottom = spacing.md),
        )
        when (plans) {
            PlusCatalogState.Loading -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
                modifier = Modifier.padding(vertical = spacing.md),
            ) {
                CircularProgressIndicator(color = VazieTheme.colors.primary)
                Text(
                    text = stringResource(R.string.onboarding_plus_loading),
                    style = VazieTheme.typography.body,
                    color = VazieTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
            is PlusCatalogState.Unavailable -> Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = stringResource(
                        if (plans.failure == AccountFailure.Unreachable) {
                            R.string.onboarding_plus_unavailable_network
                        } else {
                            R.string.onboarding_plus_unavailable
                        },
                    ),
                    style = VazieTheme.typography.body,
                    color = VazieTheme.colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
                VazieButton(
                    text = stringResource(R.string.onboarding_plus_retry),
                    onClick = onRetry,
                    variant = VazieButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            is PlusCatalogState.Ready -> Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                val locale = LocalConfiguration.current.locales[0]
                plans.catalog.plans.forEach { plan ->
                    val betterValue = plans.catalog.isBetterValue(plan)
                    VaziePlanChoice(
                        period = stringResource(
                            when (plan.billingPeriod) {
                                PlusBillingPeriod.MONTHLY -> R.string.onboarding_plus_month
                                PlusBillingPeriod.YEARLY -> R.string.onboarding_plus_year
                            },
                        ),
                        price = plan.price.format(locale),
                        note = plan.price.perMonth(plan.months)?.takeIf { betterValue }
                            ?.let { stringResource(R.string.onboarding_plus_per_month, it.format(locale)) },
                        badge = if (betterValue) stringResource(R.string.onboarding_plus_year_badge) else null,
                        selected = plan == selected,
                        onSelect = { onSelect(plan.code) },
                    )
                }
                if (plans.stale) {
                    Text(
                        text = stringResource(R.string.onboarding_plus_stale),
                        style = VazieTheme.typography.caption,
                        color = VazieTheme.colors.textSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        // No purchase without the offer accepted; the link opens it on the Vazie site.
        VazieConsentCheck(
            checked = consent,
            onCheckedChange = onConsentChange,
            text = stringResource(R.string.onboarding_plus_consent_text),
            linkText = stringResource(R.string.onboarding_plus_consent_link),
            url = LocalVazieSite.current.offer,
            modifier = Modifier.padding(top = spacing.sm),
        )
        Text(
            text = stringResource(R.string.onboarding_plus_terms),
            style = VazieTheme.typography.caption,
            color = VazieTheme.colors.textSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = spacing.sm),
        )
    }
}

