package app.vazie.vpn.feature.onboarding.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import app.vazie.vpn.core.designsystem.component.VazieConsentCheck
import app.vazie.vpn.core.designsystem.component.VaziePlanChoice
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.onboarding.R

/** The last onboarding page: VPN Plus and its two plans. The page chooses a plan; the buttons under the pager
 * act on it. */
@Composable
internal fun OnboardingPlusContent(
    yearly: Boolean,
    onSelect: (yearly: Boolean) -> Unit,
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
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            VaziePlanChoice(
                period = stringResource(R.string.onboarding_plus_month),
                price = stringResource(R.string.onboarding_plus_price_month),
                selected = !yearly,
                onSelect = { onSelect(false) },
            )
            VaziePlanChoice(
                period = stringResource(R.string.onboarding_plus_year),
                price = stringResource(R.string.onboarding_plus_price_year),
                note = stringResource(R.string.onboarding_plus_year_note),
                badge = stringResource(R.string.onboarding_plus_year_badge),
                selected = yearly,
                onSelect = { onSelect(true) },
            )
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

