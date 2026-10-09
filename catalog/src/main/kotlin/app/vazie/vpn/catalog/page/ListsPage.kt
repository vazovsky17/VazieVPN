package app.vazie.vpn.catalog.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.component.VazieBadge
import app.vazie.vpn.core.designsystem.component.VazieBadgeTone
import app.vazie.vpn.core.designsystem.component.VazieDetailRow
import app.vazie.vpn.core.designsystem.component.VazieDivider
import app.vazie.vpn.core.designsystem.component.VazieListCard
import app.vazie.vpn.core.designsystem.component.VazieListItem
import app.vazie.vpn.core.designsystem.component.VazieMark
import app.vazie.vpn.core.designsystem.component.VazieSectionHeader
import app.vazie.vpn.core.designsystem.component.VazieStatusChip
import app.vazie.vpn.core.designsystem.component.VazieStatusDot
import app.vazie.vpn.core.designsystem.component.VazieStatusTone
import app.vazie.vpn.core.designsystem.icon.VazieIcons
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@Composable
internal fun ListsPage() {
    CatalogPage {
        item {
            Specimen(stringResource(R.string.catalog_group_status)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                ) {
                    VazieStatusTone.entries.forEach { tone ->
                        VazieStatusChip(label = tone.name, tone = tone)
                    }
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_badges)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs),
                ) {
                    VazieBadge("WIREGUARD", tone = VazieBadgeTone.Muted)
                    VazieBadge("XRAY", tone = VazieBadgeTone.Muted)
                    VazieBadge("VLESS", tone = VazieBadgeTone.Strong)
                    VazieBadge("REALITY", tone = VazieBadgeTone.Muted)
                }
            }
        }
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_list_item),
                note = stringResource(R.string.catalog_note_list_item),
            ) {
                VazieListCard {
                    VazieListItem(
                        title = stringResource(R.string.catalog_list_config_title),
                        subtitle = stringResource(R.string.catalog_list_config_subtitle),
                        leadingContent = {
                            VazieMark(tone = VazieBadgeTone.Strong) {
                                Text(
                                    "VL",
                                    style = VazieTheme.typography.monoSmall,
                                    color = VazieTheme.colors.onAccentContainer,
                                )
                            }
                        },
                        trailingContent = { VazieStatusDot(VazieStatusTone.Success) },
                        onClick = {},
                    )
                    VazieDivider()
                    VazieListItem(
                        title = stringResource(R.string.catalog_list_config_alt_title),
                        subtitle = stringResource(R.string.catalog_list_config_alt_subtitle),
                        leadingContent = {
                            VazieMark {
                                Text(
                                    "WG",
                                    style = VazieTheme.typography.monoSmall,
                                    color = VazieTheme.colors.engineBadgeFg,
                                )
                            }
                        },
                        onClick = {},
                    )
                    VazieDivider()
                    VazieListItem(
                        title = stringResource(R.string.catalog_state_disabled),
                        subtitle = stringResource(R.string.catalog_list_locked),
                        leadingContent = { VazieMark { } },
                        enabled = false,
                        onClick = {},
                    )
                    VazieDivider()
                    VazieListItem(
                        title = stringResource(R.string.catalog_list_setting),
                        trailingContent = {
                            Icon(
                                imageVector = VazieIcons.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(VazieTheme.spacing.lg),
                            )
                        },
                        onClick = {},
                    )
                }
            }
        }
        item {
            Specimen(
                title = stringResource(R.string.catalog_group_detail_row),
                note = stringResource(R.string.catalog_note_detail_row),
            ) {
                VazieListCard {
                    VazieDetailRow(
                        label = stringResource(R.string.catalog_detail_protocol),
                        value = stringResource(R.string.catalog_detail_protocol_value),
                    )
                    VazieDivider()
                    VazieDetailRow(
                        label = stringResource(R.string.catalog_detail_server),
                        value = stringResource(R.string.catalog_detail_server_value),
                    )
                    VazieDivider()
                    VazieDetailRow(
                        label = stringResource(R.string.catalog_detail_last_used),
                        value = stringResource(R.string.catalog_detail_last_used_value),
                        technical = false,
                    )
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_section_header)) {
                VazieSectionHeader(
                    title = stringResource(R.string.catalog_section_example),
                    action = {
                        Text(
                            text = stringResource(R.string.catalog_action_see_all),
                            style = VazieTheme.typography.labelSmall,
                            color = VazieTheme.colors.primaryText,
                        )
                    },
                )
            }
        }
    }
}
