package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenProvider
import app.vazie.vpn.core.network.VazieApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking

/** The plans on screen: loading, ready, stale and unavailable — and that the last good answer survives a restart.
 * Real time, not `runTest`'s virtual clock: the repository's timeout must not fire while a mock answers. */
class VaziePlusCatalogRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()
    private val directory = File.createTempFile("plus-catalog", "").apply { delete(); mkdirs() }
    private val file = File(directory, "plus-catalog.json")

    @Test
    fun `before anything is read the state is loading`() {
        assertEquals(PlusCatalogState.Loading, repository { ok() }.state.value)
    }

    @Test
    fun `plans are read from the plans endpoint for the VPN product`() = runBlocking<Unit> {
        val repository = repository { ok() }

        val result = assertIs<AccountResult.Success<*>>(repository.refresh())

        assertEquals(2, (result.value as app.vazie.vpn.account.api.PlusCatalog).plans.size)
        val request = requests.single()
        assertEquals("/api/v1/plans", request.url.encodedPath)
        assertEquals("vpn", request.url.parameters["product"])
        assertNull(request.headers[HttpHeaders.Authorization], "the plans are public; nothing here needs a token")
        val ready = assertIs<PlusCatalogState.Ready>(repository.state.value)
        assertEquals(false, ready.stale)
    }

    @Test
    fun `a new price replaces the old one on the next read`() = runBlocking<Unit> {
        var amount = 29_900
        val repository = repository { ok(monthlyAmount = amount) }
        repository.refresh()
        amount = 34_900
        repository.refresh()

        val ready = assertIs<PlusCatalogState.Ready>(repository.state.value)
        assertEquals(34_900, ready.catalog.plans.first().price.amountMinorUnits)
        assertEquals(false, ready.stale)
    }

    @Test
    fun `when the backend cannot be reached the last catalogue in memory stays, marked stale`() = runBlocking<Unit> {
        var offline = false
        val repository = repository { if (offline) throw java.io.IOException("offline") else ok() }
        repository.refresh()
        offline = true

        val result = repository.refresh()

        assertEquals(AccountResult.Failure(AccountFailure.Unreachable), result, "a payment must see the failure, not the stored copy")
        val ready = assertIs<PlusCatalogState.Ready>(repository.state.value)
        assertEquals(true, ready.stale)
        assertEquals(2, ready.catalog.plans.size)
    }

    @Test
    fun `after a restart the stored catalogue is shown stale when the backend is down`() = runBlocking<Unit> {
        repository { ok() }.refresh()
        assertTrue(file.exists(), "the good answer was not kept")

        val restarted = repository { throw java.io.IOException("offline") }
        restarted.refresh()

        val ready = assertIs<PlusCatalogState.Ready>(restarted.state.value)
        assertEquals(true, ready.stale)
        assertEquals(listOf("VPN_PLUS_MONTHLY", "VPN_PLUS_YEARLY"), ready.catalog.plans.map { it.code })
    }

    @Test
    fun `with nothing stored and no answer the plans are unavailable, never invented`() = runBlocking<Unit> {
        val repository = repository { throw java.io.IOException("offline") }

        val result = repository.refresh()

        assertEquals(AccountResult.Failure(AccountFailure.Unreachable), result)
        assertEquals(PlusCatalogState.Unavailable(AccountFailure.Unreachable), repository.state.value)
    }

    @Test
    fun `a server error is unavailable too, and retry recovers`() = runBlocking<Unit> {
        var failing = true
        val repository = repository { if (failing) respondJson("{}", HttpStatusCode.InternalServerError) else ok() }

        repository.refresh()
        assertIs<PlusCatalogState.Unavailable>(repository.state.value)

        failing = false
        repository.refresh()
        assertEquals(false, assertIs<PlusCatalogState.Ready>(repository.state.value).stale)
    }

    @Test
    fun `an answer that is no catalogue is unavailable and does not replace a good stored one`() = runBlocking<Unit> {
        var body = PLANS
        val first = repository { respondJson(body, HttpStatusCode.OK) }
        first.refresh()

        body = """{"plans":[]}"""
        val second = repository { respondJson(body, HttpStatusCode.OK) }
        val result = second.refresh()

        assertEquals(AccountResult.Failure(AccountFailure.Unknown), result)
        val ready = assertIs<PlusCatalogState.Ready>(second.state.value)
        assertEquals(true, ready.stale, "the stored catalogue should stand in for an empty answer")
        assertEquals(2, ready.catalog.plans.size)
    }

    @Test
    fun `a torn stored file is no catalogue`() = runBlocking<Unit> {
        file.writeText("{\"plans\":[{\"code\":")

        val repository = repository { throw java.io.IOException("offline") }
        repository.refresh()

        assertIs<PlusCatalogState.Unavailable>(repository.state.value)
    }

    @Test
    fun `a slow backend is given up on rather than waited for`() = runBlocking<Unit> {
        val never = CompletableDeferred<Unit>()
        val repository = repository(timeoutMillis = 1) { never.await(); ok() }

        val result = repository.refresh()

        assertEquals(AccountResult.Failure(AccountFailure.Unreachable), result)
        never.complete(Unit)
    }

    private fun repository(
        timeoutMillis: Long = 10_000,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = VaziePlusCatalogRepository(
        api = VazieApiClient.create(
            engine = MockEngine { request ->
                requests += request
                handler(request)
            },
            baseUrl = ApiBaseUrl("https://vazie.example/api/v1"),
            // As `AccountStorage.plusCatalog` builds it: the plans are public, so the client carries no session.
            tokens = SessionTokenProvider { null },
        ),
        store = FilePlusCatalogStore(file),
        timeoutMillis = timeoutMillis,
    )

    private fun MockRequestHandleScope.ok(monthlyAmount: Int = 29_900) =
        respondJson(PLANS.replace("29900", monthlyAmount.toString()), HttpStatusCode.OK)

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode) =
        respond(body, status, headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"))

    private companion object {
        const val PLANS = """{"plans":[
            {"code":"VPN_PLUS_MONTHLY","title":"Vazie VPN Plus — доступ на 1 месяц","product":"VPN","productName":"Vazie VPN","billingPeriod":"MONTHLY","months":1,"amountMinorUnits":29900,"currency":"RUB","autoRenewAllowed":false},
            {"code":"VPN_PLUS_YEARLY","title":"Vazie VPN Plus — доступ на 1 год","product":"VPN","productName":"Vazie VPN","billingPeriod":"YEARLY","months":12,"amountMinorUnits":299000,"currency":"RUB","autoRenewAllowed":false}
        ],"purchase":{"mode":"PROVIDER"}}"""
    }
}
