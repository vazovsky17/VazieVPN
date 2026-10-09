package app.vazie.vpn.feature.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.vazie.vpn.core.designsystem.component.VazieButton
import app.vazie.vpn.core.designsystem.component.VazieScreenScaffold
import app.vazie.vpn.core.designsystem.component.vazieScrollEdgePadding
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.config.components.AddConfigTopBar
import app.vazie.vpn.feature.config.components.ConfigPreviewCard
import app.vazie.vpn.feature.config.components.DetectionChip
import app.vazie.vpn.feature.config.presentation.message

/** Step 2: hand Vazie a link, and see what it made of it. */
@Composable
fun ImportPreviewScreen(
    state: AddConfigUiState,
    onAction: (AddConfigAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = VazieTheme.spacing

    VazieScreenScaffold(
        modifier = modifier,
        topBar = {
            AddConfigTopBar(
                title = stringResource(R.string.config_preview_title),
                onClose = { onAction(AddConfigAction.Close) },
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = spacing.screenHorizontal)
                .vazieScrollEdgePadding(),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(spacing.md),
            ) {
                if (state.detection !is DetectionUiState.AwaitingInput) {
                    DetectionChip(detection = state.detection)
                }

                when (val detection = state.detection) {
                    DetectionUiState.AwaitingInput -> {
                        Text(
                            text = stringResource(R.string.config_paste_title),
                            style = VazieTheme.typography.title,
                            color = VazieTheme.colors.textPrimary,
                        )
                        Text(
                            text = stringResource(R.string.config_paste_body),
                            style = VazieTheme.typography.body,
                            color = VazieTheme.colors.textSecondary,
                        )
                    }

                    is DetectionUiState.Recognized -> ConfigPreviewCard(
                        preview = detection.preview,
                        name = state.name.ifBlank { detection.preview.suggestedName },
                    )

                    is DetectionUiState.Unsupported -> Text(
                        text = detection.feature.message(),
                        style = VazieTheme.typography.body,
                        color = VazieTheme.colors.textSecondary,
                    )

                    is DetectionUiState.Invalid -> Text(
                        text = detection.reason.message(),
                        style = VazieTheme.typography.body,
                        color = VazieTheme.colors.errorText,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.screenHorizontal, vertical = spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            if (state.detection is DetectionUiState.Recognized) {
                VazieButton(
                    text = stringResource(R.string.config_action_continue),
                    onClick = { onAction(AddConfigAction.Continue) },
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                VazieButton(
                    text = stringResource(R.string.config_action_paste),
                    onClick = { onAction(AddConfigAction.PasteRequested) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
