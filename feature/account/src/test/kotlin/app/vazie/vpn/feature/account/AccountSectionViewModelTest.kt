package app.vazie.vpn.feature.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AccountSectionViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `signed out offers sign-in`() = runTest(dispatcher) {
        val viewModel = subscribed(FakeAccountRepository(AccountState.SignedOut))

        assertEquals(AccountUi.SignedOut, viewModel.state.value.account)
    }

    @Test
    fun `signed in shows the address and the status`() = runTest(dispatcher) {
        val viewModel = subscribed(
            FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT)),
        )

        assertEquals(
            AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE"),
            viewModel.state.value.account,
        )
    }

    @Test
    fun `before the first answer it is checking, and offline is its own state`() = runTest(dispatcher) {
        assertEquals(AccountUi.Checking, subscribed(FakeAccountRepository(AccountState.Unknown)).state.value.account)
        assertEquals(
            AccountUi.Offline(email = null),
            subscribed(FakeAccountRepository(AccountState.Offline(null))).state.value.account,
        )
    }

    @Test
    fun `signing out returns the block to signed out`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT))
        val viewModel = subscribed(account)

        viewModel.onAction(AccountSectionAction.SignOut)
        runCurrent()

        assertEquals(1, account.signOuts)
        assertEquals(AccountUi.SignedOut, viewModel.state.value.account)
        assertFalse(viewModel.state.value.offerLocalSignOut)
    }

    @Test
    fun `a sign-out the backend cannot hear offers signing out on this device`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT))
        account.signOutResult = AccountResult.Failure(AccountFailure.Unreachable)
        val viewModel = subscribed(account)

        viewModel.onAction(AccountSectionAction.SignOut)
        runCurrent()

        assertTrue(viewModel.state.value.offerLocalSignOut)
        assertEquals(0, account.localSignOuts, "signed out locally without asking")

        viewModel.onAction(AccountSectionAction.ConfirmLocalSignOut)
        runCurrent()

        assertEquals(1, account.localSignOuts)
        assertEquals(AccountUi.SignedOut, viewModel.state.value.account)
        assertFalse(viewModel.state.value.offerLocalSignOut)
    }

    @Test
    fun `declining a local sign-out keeps the session`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT))
        account.signOutResult = AccountResult.Failure(AccountFailure.Unreachable)
        val viewModel = subscribed(account)
        viewModel.onAction(AccountSectionAction.SignOut)
        runCurrent()

        viewModel.onAction(AccountSectionAction.DismissLocalSignOut)
        runCurrent()

        assertEquals(0, account.localSignOuts)
        assertFalse(viewModel.state.value.offerLocalSignOut)
        assertTrue(viewModel.state.value.account is AccountUi.SignedIn)
    }

    @Test
    fun `retry from offline asks the backend again`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.Offline(null))
        val viewModel = subscribed(account)

        viewModel.onAction(AccountSectionAction.Retry)
        runCurrent()

        assertEquals(1, account.refreshes)
        assertTrue(viewModel.state.value.account is AccountUi.SignedIn)
    }

    @Test
    fun `deleting needs the account's own address typed, and nothing else deletes`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT))
        val viewModel = subscribed(account)

        viewModel.onAction(AccountSectionAction.ConfirmDelete)
        runCurrent()
        assertEquals(0, account.deletions)

        viewModel.onAction(AccountSectionAction.DeleteConfirmationChanged("someone@example.com"))
        runCurrent()
        assertFalse(viewModel.state.value.deleteReady)
        viewModel.onAction(AccountSectionAction.ConfirmDelete)
        runCurrent()
        assertEquals(0, account.deletions)

        viewModel.onAction(AccountSectionAction.DeleteConfirmationChanged("  Person@Example.com "))
        runCurrent()
        assertTrue(viewModel.state.value.deleteReady)
        viewModel.onAction(AccountSectionAction.ConfirmDelete)
        runCurrent()
        assertEquals(1, account.deletions)
        assertEquals(AccountUi.SignedOut, viewModel.state.value.account)
    }

    @Test
    fun `a deletion that did not happen keeps the account and says so`() = runTest(dispatcher) {
        val account = FakeAccountRepository(AccountState.SignedIn(FakeAccountRepository.ACCOUNT))
        account.deleteResult = AccountResult.Failure(AccountFailure.Unreachable)
        val viewModel = subscribed(account)

        viewModel.onAction(AccountSectionAction.DeleteConfirmationChanged("person@example.com"))
        viewModel.onAction(AccountSectionAction.ConfirmDelete)
        runCurrent()

        assertTrue(viewModel.state.value.deleteFailed)
        assertTrue(viewModel.state.value.account is AccountUi.SignedIn)
        assertFalse(viewModel.state.value.busy)
    }

    @Test
    fun `the typed address never reaches a log`() {
        val state = AccountSectionUiState(deleteConfirmation = "person@example.com")
        assertFalse(state.toString().contains("person@"))
        assertFalse(AccountSectionAction.DeleteConfirmationChanged("person@example.com").toString().contains("person@"))
    }

    @Test
    fun `the signed-in block never prints the address`() {
        assertFalse(AccountUi.SignedIn("person@example.com", "ACTIVE").toString().contains("person@"))
        assertFalse(AccountUi.Offline("person@example.com").toString().contains("person@"))
    }

    private fun TestScope.subscribed(account: FakeAccountRepository): AccountSectionViewModel {
        val viewModel = AccountSectionViewModel(account)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
        runCurrent()
        return viewModel
    }
}
