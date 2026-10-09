package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.account.api.PlusSubscription
import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenProvider
import app.vazie.vpn.core.network.VazieApiClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** Reading the subscription: the plan, its state and its end date, and which states still give access. */
class VaziePlusSubscriptionRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `an active subscription is read with its plan and end date`() = runTest {
        val repository = repository {
            respondJson(
                """{"plan":"VPN_PLUS_YEARLY","planDisplayName":"VPN Plus — год","product":"VPN_PLUS",""" +
                    """"state":"active","expiresAt":"2027-09-26T00:00:00Z","autoRenew":false,"entitlements":[]}""",
            )
        }

        val subscription = assertIs<AccountResult.Success<PlusSubscription>>(repository.current()).value

        assertEquals("VPN_PLUS_YEARLY", subscription.planCode)
        assertEquals("VPN Plus — год", subscription.planName)
        assertEquals("2027-09-26T00:00:00Z", subscription.expiresAt)
        assertTrue(subscription.active)
        val request = requests.single()
        assertEquals(HttpMethod.Get, request.method)
        assertEquals("/api/v1/subscription", request.url.encodedPath)
        assertEquals("Bearer $TOKEN", request.headers[HttpHeaders.Authorization])
    }

    @Test
    fun `the free plan and an expired subscription are not active`() {
        assertFalse(PlusSubscription("FREE", "Free", "active", null).active)
        assertFalse(PlusSubscription("VPN_PLUS_MONTHLY", "VPN Plus", "expired", "2026-01-01T00:00:00Z").active)
        assertTrue(PlusSubscription("VPN_PLUS_MONTHLY", "VPN Plus", "in_grace", "2026-01-01T00:00:00Z").active)
    }

    @Test
    fun `access granted in the admin panel is active with its end date, though there is no paid plan`() = runTest {
        val repository = repository {
            respondJson(
                """{"plan":"FREE","planDisplayName":"Free","state":"none","entitlements":""" +
                    """[{"key":"MANAGED_SERVER_ACCESS","validUntil":"2027-10-07T00:00:00Z"}]}""",
            )
        }

        val subscription = assertIs<AccountResult.Success<PlusSubscription>>(repository.current()).value

        assertTrue(subscription.active, "Settings says «Активна до …» — so must the subscription screen")
        assertEquals("2027-10-07T00:00:00Z", subscription.activeUntil)
        assertFalse(subscription.hasPlan)
    }

    @Test
    fun `open-ended access has no end date, and the free plan alone is not active`() {
        val open = PlusSubscription("FREE", "Free", "none", null, PlusAccess(validUntil = null))
        assertTrue(open.active)
        assertEquals(null, open.activeUntil)
        assertFalse(PlusSubscription("FREE", "Free", "none", null).active)
    }

    @Test
    fun `an expired session asks to sign in again`() = runTest {
        val repository = repository {
            respond(
                content = """{"error":{"code":"AUTH_REQUIRED","message":"m","requestId":"r"}}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"),
            )
        }

        assertEquals(
            AccountFailure.SessionExpired,
            assertIs<AccountResult.Failure>(repository.current()).reason,
        )
    }

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = VaziePlusSubscriptionRepository(
        api = VazieApiClient.create(
            engine = MockEngine { request ->
                requests += request
                handler(request)
            },
            baseUrl = ApiBaseUrl("https://vazie.example/api/v1"),
            tokens = SessionTokenProvider { Secret.of(TOKEN) },
        ),
    )

    private fun MockRequestHandleScope.respondJson(body: String) =
        respond(body, HttpStatusCode.OK, headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"))

    private companion object {
        const val TOKEN = "a-synthetic-session-token-that-opens-nothing"
    }
}
