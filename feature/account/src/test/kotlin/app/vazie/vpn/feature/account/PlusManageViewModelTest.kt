package app.vazie.vpn.feature.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PlusSubscription
import app.vazie.vpn.account.api.PlusSubscriptionRepository
import java.time.ZoneOffset
import java.util.Locale
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

/** The subscription screen: it loads the subscription, refreshes the account alongside, and a retry after a
 * failure asks again. */
@OptIn(ExperimentalCoroutinesApi::class)
class PlusManageViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val account = FakeAccountRepository()
    private val subscriptions = FakeSubscriptions()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `the subscription is loaded and the account refreshed`() = runTest(dispatcher) {
        subscriptions.result = AccountResult.Success(ACTIVE)

        val viewModel = PlusManageViewModel(subscriptions, account)
        assertEquals(PlusManageUiState.Loading, viewModel.state.value)
        advanceUntilIdle()

        assertEquals(PlusManageUiState.Loaded(ACTIVE), viewModel.state.value)
        assertEquals(1, account.refreshes)
    }

    @Test
    fun `a failure can be retried`() = runTest(dispatcher) {
        subscriptions.result = AccountResult.Failure(AccountFailure.Unreachable)
        val viewModel = PlusManageViewModel(subscriptions, account)
        advanceUntilIdle()
        assertEquals(PlusManageUiState.Problem(AccountFailure.Unreachable), viewModel.state.value)

        subscriptions.result = AccountResult.Success(ACTIVE)
        viewModel.retry()
        advanceUntilIdle()

        assertEquals(PlusManageUiState.Loaded(ACTIVE), viewModel.state.value)
        assertEquals(2, subscriptions.calls)
    }

    @Test
    fun `a backend date reads as a date in the person's language`() {
        assertEquals(
            "26 сентября 2027 г.",
            plusDate("2027-09-26T10:00:00Z", Locale.forLanguageTag("ru"), ZoneOffset.UTC)
                ?.replace('\u00A0', ' ')?.replace('\u202F', ' '),
        )
        assertEquals(
            "September 26, 2027",
            plusDate("2027-09-26T10:00:00Z", Locale.US, ZoneOffset.UTC),
        )
        assertNull(plusDate(null))
        assertNull(plusDate("not a date"))
    }

    private class FakeSubscriptions : PlusSubscriptionRepository {
        var result: AccountResult<PlusSubscription> = AccountResult.Failure(AccountFailure.Unknown)
        var calls = 0

        override suspend fun current(): AccountResult<PlusSubscription> {
            calls++
            return result
        }
    }

    private companion object {
        val ACTIVE = PlusSubscription(
            planCode = "VPN_PLUS_YEARLY",
            planName = "VPN Plus — год",
            state = "active",
            expiresAt = "2027-09-26T00:00:00Z",
        )
    }
}
