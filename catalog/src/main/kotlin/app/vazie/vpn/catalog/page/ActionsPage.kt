package app.vazie.vpn.catalog.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.component.VazieBottomBar
import app.vazie.vpn.core.designsystem.component.VazieBottomBarItem
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieIconButton
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@Composable
internal fun ActionsPage() {
    CatalogPage {
        item {
            Specimen(stringResource(R.string.catalog_group_buttons)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                ) {
                    VazieButtonVariant.entries.forEach { variant ->
                        VazieButton(text = variant.name, onClick = {}, variant = variant)
                    }
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_button_states)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                ) {
                    VazieButton(
                        text = stringResource(R.string.catalog_state_loading),
                        onClick = {},
                        loading = true,
                    )
                    VazieButton(
                        text = stringResource(R.string.catalog_state_disabled),
                        onClick = {},
                        enabled = false,
                    )
                    VazieButton(
                        text = stringResource(R.string.catalog_state_disabled),
                        onClick = {},
                        variant = VazieButtonVariant.Secondary,
                        enabled = false,
                    )
                    VazieButton(
                        text = stringResource(R.string.catalog_state_with_icon),
                        onClick = {},
                        variant = VazieButtonVariant.Secondary,
                        leadingContent = {
                            Icon(
                                imageVector = VazieIcons.Plus,
                                contentDescription = null,
                                modifier = Modifier.size(VazieTheme.spacing.md),
                            )
                        },
                    )
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_icon_button)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs)) {
                    VazieIconButton(
                        onClick = {},
                        contentDescription = stringResource(R.string.catalog_action_add),
                    ) {
                        Icon(
                            imageVector = VazieIcons.Plus,
                            contentDescription = null,
                            modifier = Modifier.size(VazieTheme.spacing.lg),
                        )
                    }
                    VazieIconButton(
                        onClick = {},
                        contentDescription = stringResource(R.string.catalog_action_close),
                        enabled = false,
                    ) {
                        Icon(
                            imageVector = VazieIcons.Close,
                            contentDescription = null,
                            modifier = Modifier.size(VazieTheme.spacing.lg),
                        )
                    }
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_bottom_bar)) {
                Box {
                    VazieBottomBar {
                        VazieBottomBarItem(
                            label = stringResource(R.string.catalog_nav_home),
                            selected = true,
                            onClick = {},
                        )
                        VazieBottomBarItem(
                            label = stringResource(R.string.catalog_nav_connections),
                            selected = false,
                            onClick = {},
                        )
                        VazieBottomBarItem(
                            label = stringResource(R.string.catalog_nav_settings),
                            selected = false,
                            onClick = {},
                        )
                    }
                }
            }
        }
    }
}
