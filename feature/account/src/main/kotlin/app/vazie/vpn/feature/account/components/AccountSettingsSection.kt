package app.vazie.vpn.feature.account.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.designsystem.component.VazieDivider
import app.vazie.vpn.core.designsystem.component.VazieListCard
import app.vazie.vpn.core.designsystem.component.VazieListItem
import app.vazie.vpn.core.designsystem.component.VaziePlusBadge
import app.vazie.vpn.core.designsystem.component.VazieSectionHeader
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.tour.VazieTourTargetId
import app.vazie.vpn.core.designsystem.tour.vazieTourTarget
import app.vazie.vpn.feature.account.AccountSectionAction
import app.vazie.vpn.feature.account.AccountSectionUiState
import app.vazie.vpn.feature.account.AccountUi
import app.vazie.vpn.feature.account.R
import app.vazie.vpn.feature.account.plusDate

/** The Vazie Account block in Settings: the signed-in account and VPN Plus (its end date, or the plans). */
@Composable
fun AccountSettingsSection(
    state: AccountSectionUiState,
    onAction: (AccountSectionAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // The whole block is what the first visit to Settings points at: sign-in and VPN Plus together.
    Column(modifier = modifier.vazieTourTarget(VazieTourTargetId.ACCOUNT_SECTION)) {
        VazieSectionHeader(title = stringResource(R.string.account_section_title))
        VazieListCard {
            val plus: PlusAccess? = when (val account = state.account) {
                AccountUi.Checking -> {
                    VazieListItem(title = stringResource(R.string.account_checking), enabled = false)
                    null
                }
                AccountUi.SignedOut -> {
                    VazieListItem(
                        title = stringResource(R.string.account_sign_in_row),
                        subtitle = stringResource(R.string.account_sign_in_row_body),
                        trailingContent = { Chevron() },
                        onClick = { onAction(AccountSectionAction.SignIn) },
                        enabled = !state.busy,
                    )
                    null
                }
                is AccountUi.SignedIn -> {
                    VazieListItem(
                        title = account.email ?: stringResource(R.string.account_no_email),
                        subtitle = stringResource(R.string.account_row_open),
                        trailingContent = { Chevron() },
                        onClick = { onAction(AccountSectionAction.OpenAccount) },
                    )
                    account.plus
                }
                is AccountUi.Offline -> {
                    VazieListItem(
                        title = account.email ?: stringResource(R.string.account_section_title),
                        subtitle = stringResource(R.string.account_offline_body),
                        subtitleMaxLines = Int.MAX_VALUE,
                        trailingContent = { Chevron() },
                        onClick = { onAction(AccountSectionAction.OpenAccount) },
                    )
                    account.plus
                }
            }
            if (state.account != AccountUi.Checking) {
                VazieDivider()
                VazieListItem(
                    title = stringResource(R.string.plus_row_title),
                    subtitle = if (plus != null) {
                        plusDate(plus.validUntil)?.let { stringResource(R.string.plus_row_active, it) }
                            ?: stringResource(R.string.plus_row_active_open)
                    } else {
                        stringResource(R.string.plus_row_body)
                    },
                    // One line, ellipsized: «Активна до …» or the plans' line, never a second row.
                    subtitleMaxLines = 1,
                    leadingContent = { VaziePlusBadge() },
                    trailingContent = { Chevron() },
                    onClick = {
                        onAction(if (plus != null) AccountSectionAction.ManagePlus else AccountSectionAction.OpenPlus)
                    },
                )
            }
        }
    }
}

@Composable
private fun Chevron() {
    Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null)
}
