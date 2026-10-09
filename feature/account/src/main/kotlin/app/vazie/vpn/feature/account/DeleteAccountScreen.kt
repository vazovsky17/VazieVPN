package app.vazie.vpn.feature.account

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieTextField
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Deleting the account, on a screen of its own: what goes, and a button that works only once the account's
 * address has been typed, so it cannot be pressed and confirmed by accident. */
@Composable
fun DeleteAccountScreen(
    state: AccountSectionUiState,
    onAction: (AccountSectionAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    val email = (state.account as? AccountUi.SignedIn)?.email.orEmpty()
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.account_delete_title),
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
                text = stringResource(R.string.account_delete_body),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.account_delete_type_prompt, email),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            VazieTextField(
                value = state.deleteConfirmation,
                onValueChange = { onAction(AccountSectionAction.DeleteConfirmationChanged(it)) },
                label = stringResource(R.string.account_email_label),
                enabled = !state.busy,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
            )
            VazieButton(
                text = stringResource(R.string.account_delete_confirm),
                onClick = { onAction(AccountSectionAction.ConfirmDelete) },
                enabled = state.deleteReady && !state.busy,
                loading = state.busy,
                variant = VazieButtonVariant.Destructive,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.deleteFailed) {
                Text(
                    text = stringResource(R.string.account_delete_failed),
                    style = VazieTheme.typography.caption,
                    color = VazieTheme.colors.errorText,
                )
            }
        }
    }
}
