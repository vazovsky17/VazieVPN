package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Paying for VPN Plus: the site is where the person pays, this screen waits for them and tells them when
 * VPN Plus is on. */
@Composable
fun PlusPaymentScreen(
    state: PlusPaymentUiState,
    onAction: (PlusPaymentAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    VazieScreenScaffold(
        modifier = modifier,
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.payment_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.account_back),
            )
        },
    ) {
        when (state) {
            PlusPaymentUiState.Opening -> Opening()
            is PlusPaymentUiState.Awaiting -> Awaiting(state, onAction, onBack)
            PlusPaymentUiState.Activated -> Outcome(
                title = stringResource(R.string.payment_activated_title),
                body = stringResource(R.string.payment_activated_body),
                primary = stringResource(R.string.payment_done) to { onAction(PlusPaymentAction.Done) },
            )
            PlusPaymentUiState.SiteNotOpened -> Outcome(
                title = stringResource(R.string.payment_site_title),
                body = stringResource(R.string.payment_site_body),
                primary = stringResource(R.string.payment_retry) to { onAction(PlusPaymentAction.OpenSiteAgain) },
                secondary = stringResource(R.string.payment_close) to onBack,
            )
        }
    }
}

@Composable
private fun Opening() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(top = VazieTheme.spacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm),
        ) {
            CircularProgressIndicator(color = VazieTheme.colors.primary)
            Text(
                text = stringResource(R.string.payment_opening),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
        }
    }
}

@Composable
private fun Awaiting(state: PlusPaymentUiState.Awaiting, onAction: (PlusPaymentAction) -> Unit, onBack: () -> Unit) {
    val spacing = VazieTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.payment_awaiting_title),
            style = VazieTheme.typography.headline,
            color = VazieTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.payment_awaiting_body),
            style = VazieTheme.typography.body,
            color = VazieTheme.colors.textSecondary,
        )
        when {
            state.checking -> Text(
                text = stringResource(R.string.payment_checking),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            state.notSeen -> Text(
                text = stringResource(R.string.payment_not_seen),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
        }
        VazieButton(
            text = stringResource(R.string.payment_check),
            onClick = { onAction(PlusPaymentAction.Check) },
            enabled = !state.checking,
            loading = state.checking,
            modifier = Modifier.fillMaxWidth(),
        )
        VazieButton(
            text = stringResource(R.string.payment_open_site),
            onClick = { onAction(PlusPaymentAction.OpenSiteAgain) },
            variant = VazieButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        VazieButton(
            text = stringResource(R.string.payment_close),
            onClick = onBack,
            variant = VazieButtonVariant.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun Outcome(
    title: String,
    body: String,
    primary: Pair<String, () -> Unit>,
    secondary: Pair<String, () -> Unit>? = null,
) {
    val spacing = VazieTheme.spacing
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(text = title, style = VazieTheme.typography.headline, color = VazieTheme.colors.textPrimary)
        Text(text = body, style = VazieTheme.typography.body, color = VazieTheme.colors.textSecondary)
        VazieButton(text = primary.first, onClick = primary.second, modifier = Modifier.fillMaxWidth())
        if (secondary != null) {
            VazieButton(
                text = secondary.first,
                onClick = secondary.second,
                variant = VazieButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
