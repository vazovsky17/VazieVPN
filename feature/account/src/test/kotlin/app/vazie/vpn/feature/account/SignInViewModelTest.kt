package app.vazie.vpn.feature.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.EmailCodeRequested
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class SignInViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val account = FakeAccountRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // ---------------------------------------------------------------- email step

    @Test
    fun `a plausible address asks for a code and moves to the code step`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(account)

        viewModel.onAction(SignInAction.EmailChanged("  person@example.com "))
        viewModel.onAction(SignInAction.RequestCode)
        runCurrent()

        assertEquals(listOf("person@example.com"), account.requestedEmails)
        val state = viewModel.state.value
        assertEquals(SignInStep.CODE, state.step)
        assertEquals("person@example.com", state.email)
        assertEquals(60, state.resendSecondsLeft)
        assertFalse(state.loading)
        assertNull(state.problem)
    }

    @Test
    fun `an address that cannot be one is refused without asking the backend`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(account)

        viewModel.onAction(SignInAction.EmailChanged("person.example.com"))
        viewModel.onAction(SignInAction.RequestCode)
        runCurrent()

        assertTrue(account.requestedEmails.isEmpty())
        assertEquals(SignInProblem.InvalidEmail, viewModel.state.value.problem)
        assertEquals(SignInStep.EMAIL, viewModel.state.value.step)
    }

    @Test
    fun `each backend answer to a code request becomes its own problem`() = runTest(dispatcher) {
        mapOf(
            AccountFailure.InvalidEmail to SignInProblem.InvalidEmail,
            AccountFailure.RateLimited(42) to SignInProblem.RateLimited(42),
            AccountFailure.EmailUnavailable to SignInProblem.MailUnavailable,
            AccountFailure.NotEnabled to SignInProblem.NotEnabled,
            AccountFailure.Unreachable to SignInProblem.Network,
            AccountFailure.Unknown to SignInProblem.Server,
        ).forEach { (failure, problem) ->
            account.requestResult = AccountResult.Failure(failure)
            val viewModel = SignInViewModel(account)

            viewModel.onAction(SignInAction.EmailChanged("person@example.com"))
            viewModel.onAction(SignInAction.RequestCode)
            runCurrent()

            assertEquals(problem, viewModel.state.value.problem, "for $failure")
            assertEquals(SignInStep.EMAIL, viewModel.state.value.step)
            assertFalse(viewModel.state.value.loading)
        }
    }

    @Test
    fun `typing clears the problem`() = runTest(dispatcher) {
        val viewModel = SignInViewModel(account)
        viewModel.onAction(SignInAction.EmailChanged("nope"))
        viewModel.onAction(SignInAction.RequestCode)

        viewModel.onAction(SignInAction.EmailChanged("nope@"))

        assertNull(viewModel.state.value.problem)
    }

    // ---------------------------------------------------------------- code step

    @Test
    fun `the code field keeps six digits and nothing else`() = runTest(dispatcher) {
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.CodeChanged("12 34-5678"))

        assertEquals("123456", viewModel.state.value.code)
        assertTrue(viewModel.state.value.canVerify)
    }

    @Test
    fun `a short code cannot be submitted`() = runTest(dispatcher) {
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.CodeChanged("123"))
        viewModel.onAction(SignInAction.Verify)
        runCurrent()

        assertTrue(account.verifiedCodes.isEmpty())
    }

    @Test
    fun `a correct code signs in and finishes`() = runTest(dispatcher) {
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.CodeChanged("493817"))
        viewModel.onAction(SignInAction.Verify)
        runCurrent()

        assertEquals(listOf("493817"), account.verifiedCodes)
        assertEquals(SignInEffect.SignedIn, viewModel.effects.first())
        assertEquals("", viewModel.state.value.code, "the code outlived its use")
    }

    @Test
    fun `a wrong, expired or exhausted code says so and clears the field`() = runTest(dispatcher) {
        account.verifyResult = AccountResult.Failure(AccountFailure.InvalidCode)
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.CodeChanged("000000"))
        viewModel.onAction(SignInAction.Verify)
        runCurrent()

        assertEquals(SignInProblem.InvalidCode, viewModel.state.value.problem)
        assertEquals("", viewModel.state.value.code)
        assertEquals(SignInStep.CODE, viewModel.state.value.step)
    }

    @Test
    fun `no network while verifying is a network problem`() = runTest(dispatcher) {
        account.verifyResult = AccountResult.Failure(AccountFailure.Unreachable)
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.CodeChanged("493817"))
        viewModel.onAction(SignInAction.Verify)
        runCurrent()

        assertEquals(SignInProblem.Network, viewModel.state.value.problem)
    }

    @Test
    fun `too many verify attempts is rate limited`() = runTest(dispatcher) {
        account.verifyResult = AccountResult.Failure(AccountFailure.RateLimited(30))
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.CodeChanged("493817"))
        viewModel.onAction(SignInAction.Verify)
        runCurrent()

        assertEquals(SignInProblem.RateLimited(30), viewModel.state.value.problem)
    }

    // ---------------------------------------------------------------- resend

    @Test
    fun `resend waits for the countdown, then sends again and restarts it`() = runTest(dispatcher) {
        val viewModel = atCodeStep()
        assertFalse(viewModel.state.value.canResend)

        viewModel.onAction(SignInAction.Resend)
        runCurrent()
        assertEquals(1, account.requestedEmails.size, "resend went out before the countdown ended")

        advanceTimeBy(60_001)
        assertEquals(0, viewModel.state.value.resendSecondsLeft)
        assertTrue(viewModel.state.value.canResend)

        account.requestResult = AccountResult.Success(EmailCodeRequested(90))
        viewModel.onAction(SignInAction.Resend)
        runCurrent()

        assertEquals(listOf("person@example.com", "person@example.com"), account.requestedEmails)
        assertTrue(viewModel.state.value.codeResent)
        assertEquals(90, viewModel.state.value.resendSecondsLeft)
    }

    @Test
    fun `resend too early takes the backend's wait`() = runTest(dispatcher) {
        val viewModel = atCodeStep()
        advanceTimeBy(60_001)
        account.requestResult = AccountResult.Failure(AccountFailure.RateLimited(25))

        viewModel.onAction(SignInAction.Resend)
        runCurrent()

        assertEquals(SignInProblem.RateLimited(25), viewModel.state.value.problem)
        assertEquals(25, viewModel.state.value.resendSecondsLeft)
        assertFalse(viewModel.state.value.canResend)
    }

    @Test
    fun `changing the address goes back to the first step`() = runTest(dispatcher) {
        val viewModel = atCodeStep()

        viewModel.onAction(SignInAction.ChangeEmail)

        assertEquals(SignInStep.EMAIL, viewModel.state.value.step)
        assertEquals("person@example.com", viewModel.state.value.email)
    }

    @Test
    fun `the state never prints the address or the code`() = runTest(dispatcher) {
        val viewModel = atCodeStep()
        viewModel.onAction(SignInAction.CodeChanged("493817"))

        val text = viewModel.state.value.toString()

        assertFalse(text.contains("person@example.com"))
        assertFalse(text.contains("493817"))
    }

    @Test
    fun `a wait reads as minutes and seconds`() {
        assertEquals("0:42", formatWait(42))
        assertEquals("1:00", formatWait(60))
        assertEquals("12:05", formatWait(725))
        assertEquals("0:00", formatWait(-3))
    }

    private fun TestScope.atCodeStep(): SignInViewModel {
        val viewModel = SignInViewModel(account)
        viewModel.onAction(SignInAction.EmailChanged("person@example.com"))
        viewModel.onAction(SignInAction.RequestCode)
        runCurrent()
        return viewModel
    }
}
