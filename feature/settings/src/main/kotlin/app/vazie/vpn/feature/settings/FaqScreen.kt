package app.vazie.vpn.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieCard
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Questions and answers — the same ones vazie.app answers, so the app and the site never disagree. The
 * refund answer links to the refunds page on the site. */
@Composable
fun FaqScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val spacing = VazieTheme.spacing
    val scroll = rememberScrollState()
    val uri = LocalUriHandler.current
    val site = LocalVazieSite.current
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.faq_title),
                onBack = onBack,
                backContentDescription = stringResource(R.string.settings_back),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = spacing.screenHorizontal)
                .vazieScrollEdgePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            FaqItems.forEach { item ->
                VazieCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                        Text(
                            text = stringResource(item.question),
                            style = VazieTheme.typography.titleSmall,
                            color = VazieTheme.colors.textPrimary,
                            modifier = Modifier.semantics { heading() },
                        )
                        Text(
                            text = stringResource(item.answer),
                            style = VazieTheme.typography.body,
                            color = VazieTheme.colors.textSecondary,
                        )
                        if (item.link != null) {
                            VazieButton(
                                text = stringResource(item.link.label),
                                onClick = { uri.openUri(site.page(item.link.path)) },
                                variant = VazieButtonVariant.Text,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** A link to a page on the Vazie site, by [path]. */
internal data class FaqLink(@param:StringRes val label: Int, val path: String)

internal data class FaqItem(
    @param:StringRes val question: Int,
    @param:StringRes val answer: Int,
    val link: FaqLink? = null,
)

/** The questions, in the site's order of concern: what, where, how long, refunds, contact. */
internal val FaqItems: List<FaqItem> = listOf(
    FaqItem(R.string.faq_what_q, R.string.faq_what_a),
    FaqItem(R.string.faq_where_q, R.string.faq_where_a),
    FaqItem(R.string.faq_renew_q, R.string.faq_renew_a),
    FaqItem(R.string.faq_end_q, R.string.faq_end_a),
    FaqItem(R.string.faq_start_q, R.string.faq_start_a),
    FaqItem(R.string.faq_refund_q, R.string.faq_refund_a, FaqLink(R.string.faq_refund_link, "/legal/refunds")),
    FaqItem(R.string.faq_contact_q, R.string.faq_contact_a),
)

