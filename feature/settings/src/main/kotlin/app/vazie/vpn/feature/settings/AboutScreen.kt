package app.vazie.vpn.feature.settings

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
import androidx.compose.ui.text.style.TextAlign
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieCard
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.VazieSectionHeader
import app.vazie.vpn.core.designsystem.component.VazieToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.settings.components.VazieDeveloperCard
import app.vazie.vpn.feature.settings.components.VazieProjectCard

/** Who made this, how to reach them, and how to help pay for it. */
@Composable
fun AboutScreen(
    state: AboutUiState,
    onAction: (AboutAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing
    // Hoisted so the scaffold can be told when something has gone under the toolbar. The scaffold
    // cannot reach for it: the container is this screen's choice, and the state belongs to it.
    val scroll = rememberScrollState()
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            VazieToolbar(
                title = stringResource(R.string.settings_about_title),
                onBack = { onAction(AboutAction.Back) },
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
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            AboutSection(title = stringResource(R.string.settings_made_by))
            VazieDeveloperCard(onOpenLink = { onAction(AboutAction.OpenLink(it)) })

            AboutSection(title = stringResource(R.string.settings_project_section))
            VazieProjectCard(onOpenLink = { onAction(AboutAction.OpenLink(it)) })

            AboutSection(title = stringResource(R.string.settings_support_section))
            SupportCard(onOpenLink = { onAction(AboutAction.OpenLink(it)) })

            BuildSignature(about = state.about, modifier = Modifier.padding(top = spacing.xl))
        }
    }
}

/** A section heading on About. */
@Composable
private fun AboutSection(title: String, modifier: Modifier = Modifier) {
    VazieSectionHeader(title = title, modifier = modifier)
}

@Composable
private fun SupportCard(onOpenLink: (AboutLink) -> Unit, modifier: Modifier = Modifier) {
    val spacing = VazieTheme.spacing
    VazieCard(modifier = modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
            Text(
                text = stringResource(R.string.settings_boosty_title),
                style = VazieTheme.typography.title,
                color = VazieTheme.colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.settings_boosty_body),
                style = VazieTheme.typography.body,
                color = VazieTheme.colors.textSecondary,
            )
            VazieButton(
                text = stringResource(R.string.settings_boosty_action),
                onClick = { onOpenLink(AboutLink.BOOSTY) },
                variant = VazieButtonVariant.Secondary,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BuildSignature(about: AboutUi, modifier: Modifier = Modifier) {
    Text(
        text = stringResource(
            R.string.settings_about,
            about.versionName,
            about.versionCode,
            about.flavor,
            about.buildType,
        ),
        style = VazieTheme.typography.caption,
        color = VazieTheme.colors.textSecondary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}