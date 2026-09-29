package app.vazie.vpn.feature.account

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieSuggestionChip
import app.vazie.vpn.core.designsystem.component.VazieTextField
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** A suggestion chip's touch target, or two lines of an error: whichever shows, the button stays put. */
private val SuggestionSlotHeight = 48.dp

/** Step one of email sign-in: the address a code goes to. */
@Composable
fun EmailScreen(
    state: SignInUiState,
    onAction: (SignInAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.account_email_title),
                onBack = { onAction(SignInAction.Close) },
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
                text = stringResource(R.string.account_email_body),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            // The field and its suggestions are one group, close together.
            Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                val error = state.problem?.let { signInProblemText(it) }
                    ?: stringResource(R.string.account_email_invalid).takeIf { state.emailCheck == EmailCheck.Invalid }
                VazieTextField(
                    value = state.email,
                    onValueChange = { onAction(SignInAction.EmailChanged(it)) },
                    label = stringResource(R.string.account_email_label),
                    placeholder = stringResource(R.string.account_email_placeholder),
                    enabled = !state.loading,
                    errorMessage = error,
                    showErrorMessage = false,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { onAction(SignInAction.RequestCode) }),
                    contentType = ContentType.EmailAddress,
                )
                val typo = state.emailCheck as? EmailCheck.Suggestion
                val completions = if (typo != null || state.loading) emptyList() else state.emailCompletions
                // One slot of fixed height for the error or the suggestions, so the button below never moves.
                Box(
                    contentAlignment = Alignment.CenterStart,
                    modifier = Modifier.fillMaxWidth().height(SuggestionSlotHeight),
                ) {
                    if (error != null) {
                        Text(
                            text = error,
                            style = VazieTheme.typography.caption,
                            color = VazieTheme.colors.errorText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        // One line that scrolls sideways.
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                        ) {
                            // One tap fixes the likely typo, or finishes the address.
                            if (typo != null) {
                                VazieSuggestionChip(
                                    text = stringResource(R.string.account_email_suggestion, typo.email),
                                    onClick = { onAction(SignInAction.EmailChanged(typo.email)) },
                                )
                            }
                            completions.forEach { address ->
                                VazieSuggestionChip(text = address, onClick = { onAction(SignInAction.EmailChanged(address)) })
                            }
                        }
                    }
                }
            }
            VazieButton(
                text = stringResource(R.string.account_email_submit),
                onClick = { onAction(SignInAction.RequestCode) },
                enabled = state.canRequestCode,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
