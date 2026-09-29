package app.vazie.vpn.feature.account

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieCard
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import kotlinx.coroutines.delay

/** Paying for VPN Plus inside the app: the provider's payment page in a WebView, and what happened after
 * it. */
@Composable
fun PlusPaymentScreen(
    state: PlusPaymentUiState,
    onAction: (PlusPaymentAction) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    BackHandler(enabled = state is PlusPaymentUiState.Paying) { onAction(PlusPaymentAction.Closed) }
    VazieScreenScaffold(
        modifier = modifier,
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.payment_title),
                onBack = if (state is PlusPaymentUiState.Paying) {
                    { onAction(PlusPaymentAction.Closed) }
                } else {
                    onBack
                },
                backContentDescription = stringResource(R.string.account_back),
            )
        },
    ) {
        when (state) {
            PlusPaymentUiState.Opening -> Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(spacing.sm)) {
                    CircularProgressIndicator(color = VazieTheme.colors.primary)
                    Text(
                        text = stringResource(R.string.payment_opening),
                        style = VazieTheme.typography.body,
                        color = VazieTheme.colors.textSecondary,
                    )
                }
            }
            is PlusPaymentUiState.Paying -> PaymentPage(
                url = state.url,
                onOutcome = { onAction(PlusPaymentAction.PageFinished(it)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
            )
            PlusPaymentUiState.Sent -> Outcome(
                title = stringResource(R.string.payment_success_title),
                body = stringResource(R.string.payment_success_body),
                primary = stringResource(R.string.payment_done) to { onAction(PlusPaymentAction.Done) },
            )
            PlusPaymentUiState.NotCompleted -> Outcome(
                title = stringResource(R.string.payment_failed_title),
                body = stringResource(R.string.payment_failed_body),
                primary = stringResource(R.string.payment_retry) to { onAction(PlusPaymentAction.Retry) },
                secondary = stringResource(R.string.payment_close) to onBack,
            )
            is PlusPaymentUiState.Problem -> Outcome(
                title = stringResource(R.string.payment_failed_title),
                body = paymentProblemText(state.failure),
                primary = stringResource(R.string.payment_retry) to { onAction(PlusPaymentAction.Retry) },
                secondary = stringResource(R.string.payment_close) to onBack,
            )
            is PlusPaymentUiState.ByContact -> ByContact(state = state, onClose = onBack)
        }
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

/** VPN Plus bought by writing to the author: the message to send, ready to copy — the period and the account
 * it is for — and the way to Telegram. */
@Composable
private fun ByContact(state: PlusPaymentUiState.ByContact, onClose: () -> Unit) {
    val spacing = VazieTheme.spacing
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val period = stringResource(if (state.plan == PlusPlan.YEARLY) R.string.plus_plan_year else R.string.plus_plan_month)
    val message = if (state.email != null) {
        stringResource(R.string.payment_contact_message, period, state.email)
    } else {
        stringResource(R.string.payment_contact_message_no_email, period)
    }
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FOR_MILLIS)
            copied = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
        verticalArrangement = Arrangement.spacedBy(spacing.sm),
    ) {
        Text(
            text = stringResource(R.string.payment_contact_title),
            style = VazieTheme.typography.headline,
            color = VazieTheme.colors.textPrimary,
        )
        Text(
            text = stringResource(R.string.payment_contact_body, state.contactLabel),
            style = VazieTheme.typography.body,
            color = VazieTheme.colors.textSecondary,
        )
        // Why it is by hand, and that it is not for ever.
        Text(
            text = stringResource(R.string.payment_contact_temporary),
            style = VazieTheme.typography.bodySecondary,
            color = VazieTheme.colors.onAccentContainer,
            modifier = Modifier
                .fillMaxWidth()
                .clip(VazieTheme.shapes.md)
                .background(VazieTheme.colors.accentContainer)
                .padding(horizontal = spacing.md, vertical = spacing.sm),
        )
        VazieCard(modifier = Modifier.fillMaxWidth()) {
            SelectionContainer {
                Text(text = message, style = VazieTheme.typography.body, color = VazieTheme.colors.textPrimary)
            }
        }
        if (state.email == null) {
            Text(
                text = stringResource(R.string.payment_contact_add_email),
                style = VazieTheme.typography.caption,
                color = VazieTheme.colors.textSecondary,
            )
        }
        VazieButton(
            text = stringResource(if (copied) R.string.payment_contact_copied else R.string.payment_contact_copy),
            onClick = {
                val clipboard = context.getSystemService(ClipboardManager::class.java)
                if (clipboard != null) {
                    clipboard.setPrimaryClip(ClipData.newPlainText(CLIP_LABEL, message))
                    copied = true
                }
            },
            variant = VazieButtonVariant.Secondary,
            modifier = Modifier.fillMaxWidth(),
        )
        VazieButton(
            text = stringResource(R.string.payment_contact_action),
            onClick = {
                // No app for the link is not worth a crash; the handle is on screen.
                runCatching { uriHandler.openUri(state.contactUrl) }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        VazieButton(
            text = stringResource(R.string.payment_close),
            onClick = onClose,
            variant = VazieButtonVariant.Text,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val CLIP_LABEL = "VPN Plus"
private const val COPIED_FOR_MILLIS = 2_000L

@Composable
private fun paymentProblemText(failure: AccountFailure): String = when (failure) {
    AccountFailure.NotEnabled -> stringResource(R.string.payment_error_not_enabled)
    AccountFailure.Unreachable -> stringResource(R.string.account_error_network)
    else -> stringResource(R.string.account_error_server)
}

/** The provider's page. A preview draws a placeholder: there is no network and no WebView there. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun PaymentPage(url: String, onOutcome: (PaymentOutcome) -> Unit, modifier: Modifier = Modifier) {
    if (LocalInspectionMode.current) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Robokassa", style = VazieTheme.typography.title, color = VazieTheme.colors.textSecondary)
        }
        return
    }
    val outcome by rememberUpdatedState(onOutcome)
    val siteHost = LocalVazieSite.current.host
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.allowFileAccess = false
                settings.allowContentAccess = false
                webViewClient = PaymentWebViewClient(context, siteHost) { outcome(it) }
                loadUrl(url)
            }
        },
        onRelease = { it.destroy() },
    )
}

/** Intercepts the end of the payment and hands bank-app links to the bank app. Everything else the provider's
 * page loads normally. */
private class PaymentWebViewClient(
    private val context: Context,
    private val siteHost: String,
    private val onOutcome: (PaymentOutcome) -> Unit,
) : WebViewClient() {

    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val url = request.url.toString()
        paymentOutcomeOf(url, siteHost)?.let {
            onOutcome(it)
            return true
        }
        val scheme = request.url.scheme?.lowercase()
        if (scheme == "http" || scheme == "https") return false
        openOutside(url)
        return true
    }

    private fun openOutside(url: String) {
        val intent = if (url.startsWith("intent:")) {
            runCatching { Intent.parseUri(url, Intent.URI_INTENT_SCHEME) }.getOrNull() ?: return
        } else {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        // Only an activity, never a component chosen by the page: `parseUri` would otherwise let a
        // page name any exported component.
        intent.addCategory(Intent.CATEGORY_BROWSABLE)
        intent.component = null
        intent.selector = null
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
            // No bank app for this link; the page offers other ways to pay.
        }
    }
}
