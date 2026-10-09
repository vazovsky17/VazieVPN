package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.PreviewParameter
import app.vazie.vpn.core.designsystem.preview.Appearances
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.designsystem.theme.VazieTheme

private val codeStep = SignInUiState(step = SignInStep.CODE, email = "you@example.com")

@VazieScreenPreview
@Composable
private fun CodeScreenPreview(@PreviewParameter(Appearances::class) appearance: Appearance) {
    VazieTheme(appearance = appearance) {
        CodeScreen(state = codeStep.copy(code = "4938", resendSecondsLeft = 42), onAction = {})
    }
}

@VazieScreenPreview
@Composable
private fun CodeScreenWrongCodePreview() {
    VazieTheme(appearance = Appearance.MILK) {
        CodeScreen(state = codeStep.copy(problem = SignInProblem.InvalidCode), onAction = {})
    }
}
