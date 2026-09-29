package app.vazie.vpn.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.core.designsystem.component.VazieDivider
import app.vazie.vpn.core.designsystem.component.VazieListCard
import app.vazie.vpn.core.designsystem.component.VazieListItem
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.VazieSectionHeader
import app.vazie.vpn.core.designsystem.component.scrolledUnderToolbar
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.tour.VazieTourTargetId
import app.vazie.vpn.core.designsystem.tour.vazieTourTarget
import app.vazie.vpn.core.model.SplitTunnel
import app.vazie.vpn.core.model.VazieAppIcon
import app.vazie.vpn.core.model.VazieAppIconPlate

/** Settings, after the parts that were screens in disguise became screens. */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onAction: (SettingsAction) -> Unit,
    modifier: Modifier = Modifier,
    accountSection: (@Composable () -> Unit)? = null,
) {
    val spacing = VazieTheme.spacing
    // Hoisted so the scaffold can be told when something has gone under the toolbar. The scaffold
    // cannot reach for it: the container is this screen's choice, and the state belongs to it.
    val scroll = rememberScrollState()
    VazieScreenScaffold(
        modifier = modifier,
        contentScrolledUnderToolbar = scroll.scrolledUnderToolbar(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = spacing.screenHorizontal,
                        vertical = spacing.md,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(spacing.sm),
            ) {
                Text(
                    text = stringResource(R.string.settings_title),
                    style = VazieTheme.typography.headline,
                    color = VazieTheme.colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scroll)
                .padding(horizontal = spacing.screenHorizontal)
                .vazieScrollEdgePadding(),
        ) {
            accountSection?.invoke()
            VazieSectionHeader(title = stringResource(R.string.settings_look_and_feel))
            VazieListCard {
                VazieListItem(
                    title = stringResource(R.string.settings_appearance),
                    subtitle = stringResource(state.appearance.labelRes) + SUMMARY_SEPARATOR +
                        stringResource(state.appIcon.labelRes),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.OpenAppearance) },
                    modifier = Modifier.vazieTourTarget(VazieTourTargetId.SETTINGS_APPEARANCE),
                )
            }

            VazieSectionHeader(title = stringResource(R.string.settings_vpn_behavior))
            VazieListCard {
                VazieListItem(
                    title = stringResource(R.string.settings_split_tunneling),
                    subtitle = splitTunnelStatus(state.splitTunnel),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.OpenSplitTunnel) },
                    modifier = Modifier.vazieTourTarget(VazieTourTargetId.SETTINGS_SPLIT_TUNNEL),
                )
            }

            // Support first, and on its own.
            VazieSectionHeader(title = stringResource(R.string.settings_help_section))
            VazieListCard {
                // Above Support: something missing is more common than something broken.
                VazieListItem(
                    title = stringResource(R.string.settings_guides_row),
                    subtitle = stringResource(R.string.settings_guides_row_body),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.OpenGuides) },
                    modifier = Modifier.vazieTourTarget(VazieTourTargetId.SETTINGS_GUIDES),
                )
                VazieDivider()
                VazieListItem(
                    title = stringResource(R.string.settings_faq_row),
                    subtitle = stringResource(R.string.settings_faq_row_body),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.OpenFaq) },
                )
                VazieDivider()
                VazieListItem(
                    title = stringResource(R.string.settings_report_bug),
                    subtitle = stringResource(R.string.settings_report_bug_body),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.ReportBug) },
                    subtitleMaxLines = Int.MAX_VALUE,
                    modifier = Modifier.vazieTourTarget(VazieTourTargetId.SETTINGS_REPORT_BUG),
                )
            }

            VazieSectionHeader(title = stringResource(R.string.settings_about_section))
            VazieListCard {
                VazieListItem(
                    title = stringResource(R.string.settings_privacy_row),
                    subtitle = stringResource(R.string.settings_privacy_row_body),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.OpenPrivacy) },
                )
                VazieDivider()
                VazieListItem(
                    title = stringResource(R.string.settings_about_row),
                    subtitle = stringResource(R.string.settings_about_row_body),
                    trailingContent = { ChevronIcon() },
                    onClick = { onAction(SettingsAction.OpenAbout) },
                )
            }
        }
    }
}

/** What split tunnelling is doing now, in the row's subtitle. */
@Composable
private fun splitTunnelStatus(split: SplitTunnel): String = when {
    !split.isActive -> stringResource(R.string.split_tunnel_status_off)
    split.mode == SplitTunnel.Mode.EXCLUDE ->
        pluralStringResource(R.plurals.split_tunnel_status_exclude, split.packages.size, split.packages.size)
    else -> pluralStringResource(R.plurals.split_tunnel_status_include, split.packages.size, split.packages.size)
}

@Composable
private fun ChevronIcon() {
    Icon(imageVector = VazieIcons.ChevronRight, contentDescription = null)
}

internal val VazieAppIcon.labelRes: Int
    get() = when (this) {
        VazieAppIcon.ORBIT -> R.string.settings_app_icon_orbit
        VazieAppIcon.LIME -> R.string.settings_app_icon_lime
        VazieAppIcon.EMERALD -> R.string.settings_app_icon_emerald
        VazieAppIcon.MAGENTA -> R.string.settings_app_icon_magenta
        VazieAppIcon.COTTON_CANDY -> R.string.settings_app_icon_cotton_candy
        VazieAppIcon.CRIMSON -> R.string.settings_app_icon_crimson
        VazieAppIcon.GOLD -> R.string.settings_app_icon_gold
        VazieAppIcon.PEARL -> R.string.settings_app_icon_pearl
        VazieAppIcon.ONYX -> R.string.settings_app_icon_onyx
        VazieAppIcon.MONOCHROME -> R.string.settings_app_icon_monochrome
    }

internal val VazieAppIconPlate.labelRes: Int
    get() = when (this) {
        VazieAppIconPlate.DEEP -> R.string.settings_app_icon_plate_deep
        VazieAppIconPlate.LIGHT -> R.string.settings_app_icon_plate_light
        VazieAppIconPlate.INK -> R.string.settings_app_icon_plate_ink
        VazieAppIconPlate.FOREST -> R.string.settings_app_icon_plate_forest
        VazieAppIconPlate.ROSE -> R.string.settings_app_icon_plate_rose
        VazieAppIconPlate.AMBER -> R.string.settings_app_icon_plate_amber
        VazieAppIconPlate.MIST -> R.string.settings_app_icon_plate_mist
    }

internal val Appearance.labelRes: Int
    get() = when (this) {
        Appearance.NIGHT_INDIGO -> R.string.settings_appearance_night_indigo
        Appearance.MILK -> R.string.settings_appearance_milk
    }

private const val SUMMARY_SEPARATOR = " · "
