package app.vazie.vpn.feature.account

import android.content.ActivityNotFoundException
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieCodeField
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Step two of email sign-in: the six digits from the letter, and a way to ask for another. */
@Composable
fun CodeScreen(
    state: SignInUiState,
    onAction: (SignInAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    val context = LocalContext.current
    // Back from the mail app with the code copied: it is filled in, and nothing else is read.
    LifecycleResumeEffect(state.code.isEmpty(), state.loading) {
        if (state.code.isEmpty() && !state.loading) {
            codeFromClipboard(context, SignInUiState.CODE_LENGTH)?.let { onAction(SignInAction.CodeChanged(it)) }
        }
        onPauseOrDispose {}
    }
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.account_code_title),
                onBack = { onAction(SignInAction.ChangeEmail) },
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
                text = stringResource(R.string.account_code_body, state.email),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            // Six cells, one digit each; the view model still filters and caps what is typed.
            VazieCodeField(
                value = state.code,
                onValueChange = { onAction(SignInAction.CodeChanged(it)) },
                label = stringResource(R.string.account_code_label),
                length = SignInUiState.CODE_LENGTH,
                enabled = !state.loading,
                errorMessage = state.problem?.let { signInProblemText(it) },
            )
            VazieButton(
                text = stringResource(R.string.account_code_submit),
                onClick = { onAction(SignInAction.Verify) },
                enabled = state.canVerify,
                loading = state.loading,
                modifier = Modifier.fillMaxWidth(),
            )
            VazieButton(
                text = stringResource(R.string.account_code_open_mail),
                onClick = { openMailApp(context) },
                enabled = !state.loading,
                variant = VazieButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            if (state.codeResent && state.problem == null) {
                Text(
                    text = stringResource(R.string.account_code_resent),
                    style = VazieTheme.typography.caption,
                    color = VazieTheme.colors.successText,
                )
            }
            VazieButton(
                text = if (state.resendSecondsLeft > 0) {
                    stringResource(R.string.account_code_resend_in, formatWait(state.resendSecondsLeft))
                } else {
                    stringResource(R.string.account_code_resend)
                },
                onClick = { onAction(SignInAction.Resend) },
                enabled = state.canResend,
                variant = VazieButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
            VazieButton(
                text = stringResource(R.string.account_code_change_email),
                onClick = { onAction(SignInAction.ChangeEmail) },
                enabled = !state.loading,
                variant = VazieButtonVariant.Text,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** The one code-shaped number on the clipboard, or `null`. Exactly one: a clipboard with a phone number and a
 * code in it is not a guess worth making. */
internal fun codeIn(text: CharSequence?, length: Int): String? {
    if (text == null) return null
    val matches = Regex("(?<!\\d)\\d{$length}(?!\\d)").findAll(text).map { it.value }.distinct().toList()
    return matches.singleOrNull()
}

private fun codeFromClipboard(context: Context, length: Int): String? {
    val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return null
    val description = clipboard.primaryClipDescription ?: return null
    if (!description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN) &&
        !description.hasMimeType(ClipDescription.MIMETYPE_TEXT_HTML)
    ) {
        return null
    }
    val item = clipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0) ?: return null
    return codeIn(item.text, length)
}

/** The mail app, to find the letter with the code. */
private fun openMailApp(context: Context) {
    val intent = Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_EMAIL)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        // No mail app: the person opens their mail however they do.
    }
}
