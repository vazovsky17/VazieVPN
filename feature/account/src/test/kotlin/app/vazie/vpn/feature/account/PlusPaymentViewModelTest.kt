package app.vazie.vpn.feature.account

import androidx.lifecycle.SavedStateHandle
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.analytics.Diagnostics
import app.vazie.vpn.core.analytics.PaymentEvent
import app.vazie.vpn.core.analytics.PaymentStage
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Paying for VPN Plus on the site: sign-in first, the site opened on the plan, and — back in the app — asking the
 * backend until VPN Plus is on. */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class PlusPaymentViewModelTest {

    private val dispatcher = StandardTestDispatcher()
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
    fun `signed out goes to sign-in first and opens no site`() = runTest(dispatcher) {
        val viewModel = viewModel(FakeAccountRepository(AccountState.SignedOut))
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.SignInFirst, viewModel.effects.first())
    }

    @Test
    fun `signed in opens the site on the chosen plan and waits for the browser`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn(), plan = YEARLY)
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.OpenSite(YEARLY), viewModel.effects.first())
        assertEquals(PlusPaymentUiState.Opening, viewModel.state.value)
        assertEquals(emptyList<PaymentEvent>(), reported)
    }

    @Test
    fun `an account not read yet is asked before deciding`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.Unknown)
        val viewModel = viewModel(account)
        advanceUntilIdle()

        assertEquals(1, account.refreshes)
        assertEquals(PlusPaymentEffect.OpenSite(MONTHLY), viewModel.effects.first())
    }

    @Test
    fun `the browser took the link, so the screen waits and reports it once`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn())
        advanceUntilIdle()
        viewModel.onAction(PlusPaymentAction.SiteOpened)
        viewModel.onAction(PlusPaymentAction.SiteOpened)

        assertEquals(PlusPaymentUiState.Awaiting(), viewModel.state.value)
        assertEquals(listOf(PaymentEvent(PaymentStage.OPENED, MONTHLY)), reported)
    }

    @Test
    fun `back from the site asks until VPN Plus is on`() = runTest(dispatcher) {
        val account = signedIn().apply { refreshAnswers = { n -> if (n >= 3) withPlus(UNTIL) else SIGNED_IN } }
        val viewModel = opened(account)

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Activated, viewModel.state.value)
        assertEquals(3, account.refreshes)
        assertEquals(10_000L, dispatcher.scheduler.currentTime, "five seconds between asks")
        assertEquals(PaymentEvent(PaymentStage.SUCCEEDED, MONTHLY), reported.last())
    }

    @Test
    fun `six asks, five seconds apart, and then the person is told it is not there yet`() = runTest(dispatcher) {
        val account = signedIn().apply { refreshAnswers = { SIGNED_IN } }
        val viewModel = opened(account)

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Awaiting(notSeen = true), viewModel.state.value)
        assertEquals(6, account.refreshes)
        assertEquals(25_000L, dispatcher.scheduler.currentTime)
    }

    @Test
    fun `coming back while an ask is running does not start another`() = runTest(dispatcher) {
        val account = signedIn().apply { refreshAnswers = { SIGNED_IN } }
        val viewModel = opened(account)

        viewModel.onAction(PlusPaymentAction.Resumed)
        dispatcher.scheduler.runCurrent()
        assertEquals(PlusPaymentUiState.Awaiting(checking = true), viewModel.state.value)
        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(6, account.refreshes)
    }

    @Test
    fun `check payment asks again after the asks ran out`() = runTest(dispatcher) {
        val account = signedIn().apply { refreshAnswers = { n -> if (n > 6) withPlus(UNTIL) else SIGNED_IN } }
        val viewModel = opened(account)
        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()
        assertEquals(PlusPaymentUiState.Awaiting(notSeen = true), viewModel.state.value)

        viewModel.onAction(PlusPaymentAction.Check)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Activated, viewModel.state.value)
    }

    @Test
    fun `resuming before the site was opened asks nothing`() = runTest(dispatcher) {
        val account = signedIn()
        val viewModel = viewModel(account)
        advanceUntilIdle()

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(0, account.refreshes)
        assertEquals(PlusPaymentUiState.Opening, viewModel.state.value)
    }

    @Test
    fun `an extension is not on until the end date moves`() = runTest(dispatcher) {
        val account = FakeAccountRepository(withPlus(UNTIL)).apply {
            refreshAnswers = { n -> if (n >= 4) withPlus(LATER) else withPlus(UNTIL) }
        }
        val viewModel = opened(account)

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Activated, viewModel.state.value)
        assertEquals(4, account.refreshes, "the access that was already there counted as paid")
    }

    @Test
    fun `an unchanged access is not a payment`() = runTest(dispatcher) {
        val account = FakeAccountRepository(withPlus(UNTIL)).apply { refreshAnswers = { withPlus(UNTIL) } }
        val viewModel = opened(account)

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Awaiting(notSeen = true), viewModel.state.value)
    }

    @Test
    fun `a session that ended meanwhile goes to sign-in`() = runTest(dispatcher) {
        val account = signedIn().apply { refreshAnswers = { AccountState.SignedOut } }
        val viewModel = opened(account)

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.SignInFirst, viewModel.effects.first())
    }

    @Test
    fun `no browser for the link is told, and asking again opens the site again`() = runTest(dispatcher) {
        val viewModel = viewModel(signedIn())
        advanceUntilIdle()

        viewModel.onAction(PlusPaymentAction.SiteNotOpened)
        assertEquals(PlusPaymentUiState.SiteNotOpened, viewModel.state.value)

        viewModel.onAction(PlusPaymentAction.OpenSiteAgain)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Opening, viewModel.state.value)
        assertEquals(List(2) { PlusPaymentEffect.OpenSite(MONTHLY) }, viewModel.effects.take(2).toList())
    }

    @Test
    fun `after the process was killed on the site, the site is not opened again`() = runTest(dispatcher) {
        val saved = SavedStateHandle(mapOf("plan" to MONTHLY, "plus.payment.opened" to true, "plus.payment.hadPlus" to false))
        val account = signedIn().apply { refreshAnswers = { withPlus(UNTIL) } }
        val viewModel = PlusPaymentViewModel(saved, account, diagnostics)
        advanceUntilIdle()
        assertEquals(PlusPaymentUiState.Awaiting(), viewModel.state.value)

        viewModel.onAction(PlusPaymentAction.Resumed)
        advanceUntilIdle()

        assertEquals(PlusPaymentUiState.Activated, viewModel.state.value)
        assertNull(withTimeoutOrNull(1) { viewModel.effects.first() }, "the site was opened a second time")
    }

    @Test
    fun `done closes the screen`() = runTest(dispatcher) {
        val viewModel = opened(signedIn())

        viewModel.onAction(PlusPaymentAction.Done)
        advanceUntilIdle()

        assertEquals(PlusPaymentEffect.Finished, viewModel.effects.first())
    }

    private fun signedIn() = FakeAccountRepository(SIGNED_IN)

    private fun withPlus(until: String?) = AccountState.SignedIn(FakeAccountRepository.ACCOUNT.copy(plus = PlusAccess(until)))

    private suspend fun TestScope.opened(account: FakeAccountRepository): PlusPaymentViewModel {
        val viewModel = viewModel(account)
        advanceUntilIdle()
        assertEquals(PlusPaymentEffect.OpenSite(MONTHLY), viewModel.effects.first())
        viewModel.onAction(PlusPaymentAction.SiteOpened)
        return viewModel
    }

    private fun viewModel(account: FakeAccountRepository, plan: String = MONTHLY) = PlusPaymentViewModel(
        savedState = SavedStateHandle(mapOf("plan" to plan)),
        account = account,
        diagnostics = diagnostics,
    )

    private companion object {
        const val MONTHLY = "VPN_PLUS_MONTHLY"
        const val YEARLY = "VPN_PLUS_YEARLY"
        const val UNTIL = "2027-01-01T00:00:00Z"
        const val LATER = "2027-02-01T00:00:00Z"
        val SIGNED_IN = AccountState.SignedIn(FakeAccountRepository.ACCOUNT)
    }
}
