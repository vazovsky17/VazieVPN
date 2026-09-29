package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusSubscription
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieCard
import app.vazie.vpn.core.designsystem.component.VazieConsentCheck
import app.vazie.vpn.core.designsystem.component.VazieDivider
import app.vazie.vpn.core.designsystem.component.VazieListCard
import app.vazie.vpn.core.designsystem.component.VazieListItem
import app.vazie.vpn.core.designsystem.component.VaziePlusBadge
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieSectionHeader
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Managing an active VPN Plus subscription: what it is and until when, extending it, and the terms. */
@Composable
fun PlusManageScreen(
    state: PlusManageUiState,
    onExtend: (PlusPlan) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.manage_title),
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
            when (state) {
                PlusManageUiState.Loading -> Box(Modifier.fillMaxWidth().padding(vertical = spacing.xl), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                        CircularProgressIndicator(color = VazieTheme.colors.primary)
                        Text(
                            text = stringResource(R.string.manage_loading),
                            style = VazieTheme.typography.body,
                            color = VazieTheme.colors.textSecondary,
                        )
                    }
                }
                is PlusManageUiState.Problem -> {
                    Text(
                        text = manageProblemText(state.failure),
                        style = VazieTheme.typography.body,
                        color = VazieTheme.colors.textSecondary,
                    )
                    VazieButton(
                        text = stringResource(R.string.account_retry),
                        onClick = onRetry,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                is PlusManageUiState.Loaded -> Loaded(subscription = state.subscription, onExtend = onExtend)
            }
            TermsLinks()
        }
    }
}

@Composable
private fun Loaded(subscription: PlusSubscription, onExtend: (PlusPlan) -> Unit) {
    val spacing = VazieTheme.spacing
    VazieCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.xs),
            ) {
                VaziePlusBadge()
                Text(
                    text = stringResource(R.string.plus_title),
                    style = VazieTheme.typography.title,
                    color = VazieTheme.colors.textPrimary,
                )
            }
            Text(
                text = when {
                    !subscription.active -> stringResource(R.string.manage_ended)
                    else -> plusDate(subscription.expiresAt)?.let { stringResource(R.string.manage_active, it) }
                        ?: stringResource(R.string.manage_active_open)
                },
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.manage_plan, subscription.planName),
                style = VazieTheme.typography.bodySecondary,
                color = VazieTheme.colors.textSecondary,
            )
        }
    }
    Text(
        text = stringResource(R.string.manage_one_off),
        style = VazieTheme.typography.bodySecondary,
        color = VazieTheme.colors.textSecondary,
    )

    var plan by rememberSaveable { mutableStateOf(PlusPlan.YEARLY) }
    var consent by rememberSaveable { mutableStateOf(false) }
    VazieSectionHeader(title = stringResource(R.string.manage_extend_title))
    Text(
        text = stringResource(R.string.manage_extend_body),
        style = VazieTheme.typography.bodySecondary,
        color = VazieTheme.colors.textSecondary,
    )
    PlusPlanChoices(selected = plan, onSelect = { plan = it })
    OfferConsent(checked = consent, onCheckedChange = { consent = it })
    VazieButton(
        text = stringResource(R.string.manage_extend),
        onClick = { onExtend(plan) },
        enabled = consent,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** The offer consent every purchase asks for. */
@Composable
internal fun OfferConsent(checked: Boolean, onCheckedChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    VazieConsentCheck(
        checked = checked,
        onCheckedChange = onCheckedChange,
        text = stringResource(R.string.plus_consent_text),
        linkText = stringResource(R.string.plus_consent_link),
        url = LocalVazieSite.current.offer,
        modifier = modifier,
    )
}

@Composable
private fun TermsLinks() {
    val uri = LocalUriHandler.current
    val site = LocalVazieSite.current
    VazieListCard {
        VazieListItem(
            title = stringResource(R.string.manage_offer),
            trailingContent = { Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null) },
            onClick = { uri.openUri(site.offer) },
        )
        VazieDivider()
        VazieListItem(
            title = stringResource(R.string.manage_refunds),
            trailingContent = { Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null) },
            onClick = { uri.openUri(site.refunds) },
        )
    }
}

@Composable
private fun manageProblemText(failure: AccountFailure): String = when (failure) {
    AccountFailure.Unreachable -> stringResource(R.string.account_error_network)
    else -> stringResource(R.string.account_error_server)
}
