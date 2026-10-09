package app.vazie.vpn.catalog.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.catalog.R
import app.vazie.vpn.core.designsystem.component.VazieFieldAction
import app.vazie.vpn.core.designsystem.component.VazieSecretField
import app.vazie.vpn.core.designsystem.component.VazieSegmentedControl
import app.vazie.vpn.core.designsystem.component.VazieSwatchPicker
import app.vazie.vpn.core.designsystem.component.VazieSwitch
import app.vazie.vpn.core.designsystem.component.VazieTextField
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.theme.colorsFor

@Composable
internal fun InputPage() {
    CatalogPage {
        item {
            val sampleName = stringResource(R.string.catalog_list_config_title)
            var text by rememberSaveable(sampleName) { mutableStateOf(sampleName) }
            Specimen(stringResource(R.string.catalog_group_text_field)) {
                Column(verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm)) {
                    VazieTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = stringResource(R.string.catalog_field_name),
                    )
                    VazieTextField(
                        value = "",
                        onValueChange = {},
                        label = stringResource(R.string.catalog_state_placeholder),
                        placeholder = stringResource(R.string.catalog_field_placeholder),
                    )
                    VazieTextField(
                        value = stringResource(R.string.catalog_field_invalid_value),
                        onValueChange = {},
                        label = stringResource(R.string.catalog_state_error),
                        errorMessage = stringResource(R.string.catalog_field_error),
                    )
                    VazieTextField(
                        value = stringResource(R.string.catalog_list_config_title),
                        onValueChange = {},
                        label = stringResource(R.string.catalog_state_disabled),
                        enabled = false,
                    )
                }
            }
        }
        item {
            var revealed by rememberSaveable { mutableStateOf(false) }
            Specimen(
                title = stringResource(R.string.catalog_group_secret_field),
                note = stringResource(R.string.catalog_note_secret_field),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.sm)) {
                    VazieSecretField(
                        value = "sample-value-not-a-real-credential",
                        revealed = revealed,
                        label = stringResource(R.string.catalog_field_secret),
                        trailingContent = {
                            VazieFieldAction(
                                label = stringResource(
                                    if (revealed) R.string.catalog_action_hide
                                    else R.string.catalog_action_show,
                                ),
                                onClick = { revealed = !revealed },
                            )
                        },
                    )
                    VazieSecretField(
                        value = "sample-value",
                        revealed = false,
                        label = stringResource(R.string.catalog_state_valid),
                        valid = true,
                    )
                }
            }
        }
        item {
            var checked by rememberSaveable { mutableStateOf(true) }
            Specimen(stringResource(R.string.catalog_group_switch)) {
                Column(verticalArrangement = Arrangement.spacedBy(VazieTheme.spacing.xs)) {
                    SpecimenRow(stringResource(R.string.catalog_state_default)) {
                        VazieSwitch(checked = checked, onCheckedChange = { checked = it })
                    }
                    SpecimenRow(stringResource(R.string.catalog_state_disabled)) {
                        VazieSwitch(checked = true, onCheckedChange = null, enabled = false)
                    }
                }
            }
        }
        item {
            var mode by rememberSaveable { mutableIntStateOf(0) }
            val segmentLabels = listOf(
                stringResource(R.string.catalog_segment_auto),
                stringResource(R.string.catalog_segment_manual),
            )
            Specimen(stringResource(R.string.catalog_group_segmented)) {
                VazieSegmentedControl(
                    options = listOf(0, 1),
                    selected = mode,
                    onSelect = { mode = it },
                    label = { segmentLabels[it] },
                )
            }
        }
        item {
            var appearance by rememberSaveable { mutableStateOf(Appearance.Default) }
            Specimen(
                title = stringResource(R.string.catalog_group_swatches),
                note = stringResource(R.string.catalog_note_swatches),
            ) {
                val options = Appearance.entries
                VazieSwatchPicker(
                    options = options,
                    selected = appearance,
                    onSelect = { appearance = it },
                    swatchColor = { colorsFor(it).background },
                    label = { it.name },
                )
            }
        }
    }
}
