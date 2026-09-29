package app.vazie.vpn.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.core.designsystem.preview.VazieScreenPreview
import app.vazie.vpn.core.designsystem.theme.VazieTheme

/** Every state of the payment screen. */
private class PaymentStates : PreviewParameterProvider<PlusPaymentUiState> {
    override val values = sequenceOf(
        PlusPaymentUiState.Opening,
        PlusPaymentUiState.Paying("https://pay.example"),
        PlusPaymentUiState.Sent,
        PlusPaymentUiState.NotCompleted,
        PlusPaymentUiState.Problem(AccountFailure.NotEnabled),
    )
}

@VazieScreenPreview
@Composable
private fun PlusPaymentScreenPreview(@PreviewParameter(PaymentStates::class) state: PlusPaymentUiState) {
    VazieTheme {
        PlusPaymentScreen(state = state, onAction = {}, onBack = {})
    }
}

@Preview(name = "ru · large font", widthDp = 320, heightDp = 760, fontScale = 1.5f, locale = "ru")
@Composable
private fun PlusPaymentScreenRussianPreview() {
    VazieTheme {
        PlusPaymentScreen(state = PlusPaymentUiState.Sent, onAction = {}, onBack = {})
    }
}
