package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

@VazieScreenPreview
@Composable
private fun EmailScreenPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        EmailScreen(state = SignInUiState(email = "you@example.com"), onAction = {})
    }
}

@VazieScreenPreview
@Composable
private fun EmailScreenInvalidPreview() {
    VazieTheme(appearance = Appearance.MILK) {
        EmailScreen(
            state = SignInUiState(email = "you@", problem = SignInProblem.InvalidEmail),
            onAction = {},
        )
    }
}
