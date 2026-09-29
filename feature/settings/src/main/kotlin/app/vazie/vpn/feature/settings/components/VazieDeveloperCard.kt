package app.vazie.vpn.feature.settings.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import app.vazie.vpn.core.designsystem.component.VazieCard
import app.vazie.vpn.core.designsystem.component.VazieDivider
import app.vazie.vpn.core.designsystem.component.VazieListItem
import app.vazie.vpn.core.designsystem.component.VazovskySticker
import app.vazie.vpn.core.designsystem.component.VazovskyStickerDefaults
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.settings.AboutLink
import app.vazie.vpn.feature.settings.R

/** Who makes Vazie, where to reach them, and where the rest of their work is. */
@Composable
internal fun VazieDeveloperCard(
    onOpenLink: (AboutLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    VazieCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.cardPadding)
                .semantics(mergeDescendants = true) { },
            horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VazovskySticker(height = VazovskyStickerDefaults.CompactHeight)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(spacing.xxs),
            ) {
                Text(
                    text = stringResource(R.string.settings_made_by_title),
                    style = VazieTheme.typography.title,
                    color = VazieTheme.colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.settings_made_by_body),
                    style = VazieTheme.typography.caption,
                    color = VazieTheme.colors.textSecondary,
                )
            }
        }
        VazieDivider()
        LinkRow(
            title = stringResource(R.string.settings_developer),
            subtitle = stringResource(R.string.settings_developer_body),
            onClick = { onOpenLink(AboutLink.DEVELOPER) },
        )
        VazieDivider()
        LinkRow(
            title = stringResource(R.string.settings_folio),
            subtitle = stringResource(R.string.settings_folio_body),
            onClick = { onOpenLink(AboutLink.FOLIO) },
        )
    }
}

/** Vazie itself: the channel, the support address and the source. Where a question about the app or a payment
 * goes, kept apart from the author's own contacts above it. */
@Composable
internal fun VazieProjectCard(
    onOpenLink: (AboutLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    VazieCard(modifier = modifier.fillMaxWidth(), contentPadding = PaddingValues()) {
        LinkRow(
            title = stringResource(R.string.settings_channel),
            subtitle = stringResource(R.string.settings_channel_body),
            onClick = { onOpenLink(AboutLink.CHANNEL) },
        )
        VazieDivider()
        LinkRow(
            title = stringResource(R.string.settings_support_email),
            subtitle = stringResource(R.string.settings_support_email_body),
            onClick = { onOpenLink(AboutLink.SUPPORT_EMAIL) },
        )
        VazieDivider()
        LinkRow(
            title = stringResource(R.string.settings_source),
            subtitle = stringResource(R.string.settings_source_body),
            onClick = { onOpenLink(AboutLink.SOURCE) },
        )
    }
}

@Composable
private fun LinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    VazieListItem(
        title = title,
        subtitle = subtitle,
        trailingContent = {
            Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null)
        },
        onClick = onClick,
    )
}
