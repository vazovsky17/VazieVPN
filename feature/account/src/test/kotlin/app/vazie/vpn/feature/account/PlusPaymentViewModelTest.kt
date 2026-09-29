package app.vazie.vpn.feature.account

import androidx.lifecycle.SavedStateHandle
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.PaymentStart
import app.vazie.vpn.account.api.PlusPaymentRepository
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPurchase
import app.vazie.vpn.core.analytics.Diagnostics
import app.vazie.vpn.core.analytics.PaymentEvent
import app.vazie.vpn.core.analytics.PaymentStage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Paying for VPN Plus: sign-in first, idempotent retries, and "sent" refreshes without granting. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlusPaymentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val payments = FakePayments()
    private val reported = mutableListOf<PaymentEvent>()
    private val diagnostics = object : Diagnostics {
        override fun payment(event: PaymentEvent) {
            reported += event
        }
    }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `signed out goes to sign-in first and opens no payment`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut))
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.SignInFirst, viewModel.effects.first())
        assertEquals(0, payments.calls.size)
    }

    @Test
    fun `signed in opens the payment page for the chosen plan`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn(), plan = PlusPlan.YEARLY)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Paying(URL), viewModel.state.value)
        assertEquals(PlusPlan.YEARLY, payments.calls.single().first)
    }

    @Test
    fun `while the backend takes no payments, the author is written to and no payment is opened`() = runTest(dispatcher) {
        payments.purchase = PlusPurchase.Contact("https://t.me/vazovsky17")
        val viewModel = viewModel(signedIn(), plan = PlusPlan.YEARLY)
        advanceUntilIdle()

        val state = assertIs<PlusPaymentUiState.ByContact>(viewModel.state.value)
        assertEquals("https://t.me/vazovsky17", state.contactUrl)
        assertEquals("@vazovsky17", state.contactLabel)
        assertEquals(PlusPlan.YEARLY, state.plan)
        assertEquals(FakeAccountRepository.ACCOUNT.email, state.email)
        assertEquals(0, payments.calls.size, "a payment was opened anyway")
    }

    @Test
    fun `signed out still signs in first while payments go through the author`() = runTest(dispatcher) {
        payments.purchase = PlusPurchase.Contact("https://t.me/vazovsky17")
        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut))
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.SignInFirst, viewModel.effects.first())
    }

    @Test
    fun `a retry after a problem asks for the same payment`() = runTest(dispatcher) {
        payments.result = AccountResult.Failure(AccountFailure.Unreachable)
        val viewModel = viewModel(signedIn())
        advanceUntilIdle()
        assertEquals(PlusPaymentUiState.Problem(AccountFailure.Unreachable), viewModel.state.value)

        payments.result = AccountResult.Success(PaymentStart("p", URL))
        viewModel.onAction(PlusPaymentAction.Retry)
        advanceUntilIdle()

        assertEquals(2, payments.calls.size)
        assertEquals(payments.calls[0].second, payments.calls[1].second, "a retry opened a second payment")
    }

    @Test
    fun `trying again after a payment that did not go through is a new purchase`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn())
        advanceUntilIdle()
        viewModel.onAction(PlusPaymentAction.PageFinished(PaymentOutcome.NotCompleted))
        assertEquals(PlusPaymentUiState.NotCompleted, viewModel.state.value)

        viewModel.onAction(PlusPaymentAction.Retry)
        advanceUntilIdle()

        assertNotEquals(payments.calls[0].second, payments.calls[1].second)
    }

    @Test
    fun `sent refreshes the account and grants nothing itself`() = runTest(dispatcher) {
        val account = signedIn()
        val viewModel = viewModel(account)
        advanceUntilIdle()
        val before = account.refreshes

        viewModel.onAction(PlusPaymentAction.PageFinished(PaymentOutcome.Sent))
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Sent, viewModel.state.value)
        assertEquals(before + 1, account.refreshes)
    }

    @Test
    fun `an expired session signs out locally and goes to sign-in`() = runTest(dispatcher) {
        payments.result = AccountResult.Failure(AccountFailure.SessionExpired)
        val account = signedIn()
        val viewModel = viewModel(account)
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.SignInFirst, viewModel.effects.first())
        assertEquals(1, account.localSignOuts)
    }

    @Test
    fun `closing the payment page mid-way is a payment not completed`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn())
        advanceUntilIdle()

        viewModel.onAction(PlusPaymentAction.Closed)

        assertIs<PlusPaymentUiState.NotCompleted>(viewModel.state.value)
    }

    @Test
    fun `each way a payment ends is reported with the plan and nothing else`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn(), plan = PlusPlan.YEARLY)
        advanceUntilIdle()
        viewModel.onAction(PlusPaymentAction.PageFinished(PaymentOutcome.Sent))

        assertEquals(
            listOf(PaymentEvent(PaymentStage.OPENED, "YEARLY"), PaymentEvent(PaymentStage.SUCCEEDED, "YEARLY")),
            reported,
        )
    }

    @Test
    fun `a payment that fails, is declined or is closed is reported as such`() = runTest(dispatcher) {
        payments.result = AccountResult.Failure(AccountFailure.Unreachable)
        val failing = viewModel(signedIn())
        advanceUntilIdle()
        assertEquals(PaymentEvent(PaymentStage.FAILED, "MONTHLY", reason = "Unreachable"), reported.last())

        payments.result = AccountResult.Success(PaymentStart("p", URL))
        failing.onAction(PlusPaymentAction.Retry)
        advanceUntilIdle()
        failing.onAction(PlusPaymentAction.PageFinished(PaymentOutcome.NotCompleted))
        assertEquals(PaymentStage.NOT_COMPLETED, reported.last().stage)

        val closing = viewModel(signedIn())
        advanceUntilIdle()
        closing.onAction(PlusPaymentAction.Closed)
        assertEquals(PaymentStage.CLOSED, reported.last().stage)
    }

    @Test
    fun `the page's end is read from vazie app's success and fail pages only`() {
        assertEquals(PaymentOutcome.Sent, paymentOutcomeOf("https://vazie.app/payment/success?InvId=1&OutSum=199.00", "vazie.app"))
        assertEquals(PaymentOutcome.NotCompleted, paymentOutcomeOf("https://vazie.app/payment/failed?InvId=1", "vazie.app"))
        assertEquals(PaymentOutcome.Sent, paymentOutcomeOf("https://www.vazie.app/payment/success/", "vazie.app"))
        assertEquals(null, paymentOutcomeOf("https://auth.robokassa.ru/Merchant/Index.aspx", "vazie.app"))
        assertEquals(null, paymentOutcomeOf("http://vazie.app/payment/success", "vazie.app"))
        assertEquals(null, paymentOutcomeOf("https://vazie.app.evil.example/payment/success", "vazie.app"))
        assertEquals(null, paymentOutcomeOf("https://vazie.app/payment/successful", "vazie.app"))
        assertEquals(null, paymentOutcomeOf("not a url", "vazie.app"))
    }

    @Test
    fun `the site host comes from the build, so another site is matched and vazie app is not`() {
        assertEquals(PaymentOutcome.Sent, paymentOutcomeOf("https://staging.example/payment/success", "staging.example"))
        assertEquals(null, paymentOutcomeOf("https://vazie.app/payment/success", "staging.example"))
    }

    private fun signedIn() = FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT))

    private fun viewModel(account: FakeAccountRepository, plan: PlusPlan = PlusPlan.MONTHLY) = PlusPaymentViewModel(
        savedState = SavedStateHandle(mapOf("plan" to plan.name)),
        account = account,
        payments = payments,
        diagnostics = diagnostics,
    )

    private class FakePayments : PlusPaymentRepository {
        var result: AccountResult<PaymentStart> = AccountResult.Success(PaymentStart("p", URL))
        val calls = mutableListOf<Pair<PlusPlan, String>>()

        override suspend fun startPayment(plan: PlusPlan, idempotencyKey: String): AccountResult<PaymentStart> {
            calls += plan to idempotencyKey
            return result
        }

        var purchase: PlusPurchase = PlusPurchase.PaymentPage

        override suspend fun purchase(): PlusPurchase = purchase
    }

    private companion object {
        const val URL = "https://auth.robokassa.ru/Merchant/Index.aspx?InvId=1&SignatureValue=synthetic"
    }
}
