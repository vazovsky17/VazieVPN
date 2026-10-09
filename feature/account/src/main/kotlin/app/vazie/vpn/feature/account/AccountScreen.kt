package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieDialog
import app.vazie.vpn.core.designsystem.component.VazieDivider
import app.vazie.vpn.core.designsystem.component.VazieListCard
import app.vazie.vpn.core.designsystem.component.VazieListItem
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** The account: its address and status, VPN Plus, and signing out. Opened from the account row in Settings;
 * when nobody is signed in any more the route leaves it. */
@Composable
fun AccountScreen(
    state: AccountSectionUiState,
    onAction: (AccountSectionAction) -> Unit,
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
                title = stringResource(R.string.account_title),
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
            val (email, status, plus, offline) = when (val account = state.account) {
                is AccountUi.SignedIn -> Quad(account.email, account.status, account.plus, false)
                is AccountUi.Offline -> Quad(account.email, null, account.plus, true)
                AccountUi.Checking, AccountUi.SignedOut -> Quad(null, null, null, false)
            }
            VazieListCard {
                VazieListItem(
                    title = stringResource(R.string.account_email_row),
                    subtitle = email ?: stringResource(R.string.account_no_email),
                )
                VazieDivider()
                VazieListItem(
                    title = stringResource(R.string.account_status_row),
                    subtitle = when {
                        offline -> stringResource(R.string.account_offline_body)
                        status == STATUS_ACTIVE -> stringResource(R.string.account_status_active)
                        status != null -> status.lowercase()
                        else -> stringResource(R.string.account_status_unconfirmed)
                    },
                    subtitleMaxLines = Int.MAX_VALUE,
                )
                VazieDivider()
                VazieListItem(
                    title = stringResource(R.string.account_plus_row),
                    subtitle = plusLine(plus),
                    trailingContent = { Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null) },
                    onClick = {
                        onAction(if (plus != null) AccountSectionAction.ManagePlus else AccountSectionAction.OpenPlus)
                    },
                )
            }
            if (offline) {
                VazieButton(
                    text = stringResource(R.string.account_retry),
                    onClick = { onAction(AccountSectionAction.Retry) },
                    enabled = !state.busy,
                    variant = VazieButtonVariant.Secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            VazieButton(
                text = stringResource(R.string.account_sign_out),
                onClick = { onAction(AccountSectionAction.SignOut) },
                enabled = !state.busy,
                loading = state.busy,
                variant = VazieButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth(),
            )
            // Apart from everything else, and only a way to the deletion screen, never the deletion itself.
            if (!offline && email != null) {
                Spacer(modifier = Modifier.height(spacing.xl))
                VazieListCard {
                    VazieListItem(
                        title = stringResource(R.string.account_delete),
                        subtitle = stringResource(R.string.account_delete_row_body),
                        trailingContent = { Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null) },
                        onClick = { onAction(AccountSectionAction.OpenDeleteAccount) },
                    )
                }
            }
        }
    }

    if (state.offerLocalSignOut) {
        VazieDialog(
            title = stringResource(R.string.account_local_sign_out_title),
            text = stringResource(R.string.account_local_sign_out_body),
            onDismissRequest = { onAction(AccountSectionAction.DismissLocalSignOut) },
            confirmButton = {
                VazieButton(
                    text = stringResource(R.string.account_local_sign_out_confirm),
                    onClick = { onAction(AccountSectionAction.ConfirmLocalSignOut) },
                    variant = VazieButtonVariant.Destructive,
                )
            },
            dismissButton = {
                VazieButton(
                    text = stringResource(R.string.account_cancel),
                    onClick = { onAction(AccountSectionAction.DismissLocalSignOut) },
                    variant = VazieButtonVariant.Text,
                )
            },
        )
    }
}

@Composable
private fun plusLine(plus: PlusAccess?): String = when {
    plus == null -> stringResource(R.string.account_plus_none)
    else -> plusDate(plus.validUntil)?.let { stringResource(R.string.plus_row_active, it) }
        ?: stringResource(R.string.plus_row_active_open)
}

private data class Quad(val email: String?, val status: String?, val plus: PlusAccess?, val offline: Boolean)

private const val STATUS_ACTIVE = "ACTIVE"
