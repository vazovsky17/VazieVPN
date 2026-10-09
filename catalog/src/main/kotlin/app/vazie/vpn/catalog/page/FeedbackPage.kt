package app.vazie.vpn.catalog.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieButtonVariant
import app.vazie.vpn.core.designsystem.component.VazieCard
import app.vazie.vpn.core.designsystem.component.VazieCircularProgress
import app.vazie.vpn.core.designsystem.component.VazieDialog
import app.vazie.vpn.core.designsystem.component.VazieDialogActions
import app.vazie.vpn.core.designsystem.component.VazieLinearProgress
import app.vazie.vpn.core.designsystem.component.VazieMark
import app.vazie.vpn.core.designsystem.component.VazieMessageState
import app.vazie.vpn.core.designsystem.component.VazieSnackbar
import app.vazie.vpn.core.designsystem.component.VazieSurfaceStyle
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@Composable
internal fun FeedbackPage() {
    CatalogPage {
        item {
            Specimen(stringResource(R.string.catalog_group_progress)) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(VazieTheme.spacing.md),
                    verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm),
                ) {
                    VazieCircularProgress()
                }
                VazieLinearProgress(progress = 0.6f)
                VazieLinearProgress()
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_snackbar)) {
                VazieSnackbar(
                    message = stringResource(R.string.catalog_snackbar_message),
                    actionLabel = stringResource(R.string.catalog_action_connect),
                    onAction = {},
                )
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_states)) {
                Column(verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm)) {
                    VazieCard {
                        VazieMessageState(
                            title = stringResource(R.string.catalog_empty_title),
                            description = stringResource(R.string.catalog_empty_description),
                            mark = { VazieMark { } },
                        )
                    }
                    VazieCard {
                        VazieMessageState(
                            title = stringResource(R.string.catalog_error_title),
                            description = stringResource(R.string.catalog_error_description),
                            mark = {
                                VazieMark {
                                    Text(
                                        "!",
                                        style = VazieTheme.typography.title,
                                        color = VazieTheme.colors.onErrorContainer,
                                    )
                                }
                            },
                            action = {
                                VazieButton(
                                    text = stringResource(R.string.catalog_action_retry),
                                    onClick = {},
                                    variant = VazieButtonVariant.Secondary,
                                )
                            },
                        )
                    }
                }
            }
        }
        item {
            Specimen(stringResource(R.string.catalog_group_surfaces)) {
                Column(verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm)) {
                    VazieSurfaceStyle.entries.forEach { style ->
                        VazieCard(style = style, modifier = Modifier) {
                            Text(
                                text = style.name,
                                style = VazieTheme.typography.body,
                                color = VazieTheme.colors.textPrimary,
                            )
                        }
                    }
                }
            }
        }
        item {
            var showDialog by rememberSaveable { mutableStateOf(false) }
            Specimen(stringResource(R.string.catalog_group_dialog)) {
                VazieButton(
                    text = stringResource(R.string.catalog_action_open_dialog),
                    onClick = { showDialog = true },
                    variant = VazieButtonVariant.Secondary,
                )
            }
            if (showDialog) {
                VazieDialog(
                    title = stringResource(R.string.catalog_dialog_title),
                    text = stringResource(R.string.catalog_dialog_text),
                    onDismissRequest = { showDialog = false },
                    confirmButton = {
                        VazieDialogActions {
                            VazieButton(
                                text = stringResource(R.string.catalog_action_cancel),
                                onClick = { showDialog = false },
                                variant = VazieButtonVariant.Text,
                            )
                            VazieButton(
                                text = stringResource(R.string.catalog_action_remove),
                                onClick = { showDialog = false },
                                variant = VazieButtonVariant.Destructive,
                            )
                        }
                    },
                )
            }
        }
    }
}
