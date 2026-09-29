package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PaymentStart
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPurchase
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
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Opening a VPN Plus payment: the plan goes by code, the idempotency key rides as a header, the session is
 * sent, and only a real https link counts as a payment someone can complete. */
class VaziePlusPaymentRepositoryTest {

    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `a payment is opened for the plan's code with the idempotency key`() = runTest {
        val repository = repository {
            respondJson(
                """{"id":"$PAYMENT_ID","status":"PENDING","plan":"VPN_PLUS_YEARLY","amountMinorUnits":299000,""" +
                    """"currency":"RUB","createdAt":"2026-09-26T00:00:00Z","confirmationUrl":"$PAY_URL"}""",
                HttpStatusCode.Created,
            )
        }

        val result = repository.startPayment(PlusPlan.YEARLY, KEY)

        assertEquals(AccountResult.Success(PaymentStart(PAYMENT_ID, PAY_URL)), result)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/payments", request.url.encodedPath)
        assertEquals(KEY, request.headers["Idempotency-Key"])
        assertEquals("Bearer $TOKEN", request.headers[HttpHeaders.Authorization])
        val body = (request.body as io.ktor.http.content.TextContent).text
        assertEquals("VPN_PLUS_YEARLY", Json.parseToJsonElement(body).jsonObject["plan"]?.jsonPrimitive?.content)
    }

    @Test
    fun `a payment without a link to complete it is not offered`() = runTest {
        val repository = repository {
            respondJson("""{"id":"$PAYMENT_ID","status":"PENDING","plan":"VPN_PLUS_MONTHLY"}""", HttpStatusCode.OK)
        }

        assertEquals(AccountFailure.Unknown, repository.startPayment(PlusPlan.MONTHLY, KEY).reason())
    }

    @Test
    fun `a link that is not https is not opened`() = runTest {
        val repository = repository {
            respondJson(
                """{"id":"$PAYMENT_ID","status":"PENDING","plan":"VPN_PLUS_MONTHLY","confirmationUrl":"http://pay.example"}""",
                HttpStatusCode.Created,
            )
        }

        assertEquals(AccountFailure.Unknown, repository.startPayment(PlusPlan.MONTHLY, KEY).reason())
    }

    @Test
    fun `an expired session asks to sign in again`() = runTest {
        val repository = repository { error("AUTH_REQUIRED", HttpStatusCode.Unauthorized) }

        assertEquals(AccountFailure.SessionExpired, repository.startPayment(PlusPlan.MONTHLY, KEY).reason())
    }

    @Test
    fun `a backend with no payment provider says payments are not enabled`() = runTest {
        val repository = repository { error("NOT_ENABLED", HttpStatusCode.NotImplemented) }

        assertEquals(AccountFailure.NotEnabled, repository.startPayment(PlusPlan.MONTHLY, KEY).reason())
    }

    @Test
    fun `contact mode is read from the plans, with its link`() = runTest {
        val repository = repository {
            respondJson("""{"plans":[],"purchase":{"mode":"CONTACT","contactUrl":"https://t.me/vazovsky17"}}""", HttpStatusCode.OK)
        }

        val purchase = assertIs<PlusPurchase.Contact>(repository.purchase())
        assertEquals("https://t.me/vazovsky17", purchase.contactUrl)
        assertEquals("@vazovsky17", purchase.contactLabel)
        assertEquals("/api/v1/plans", requests.single().url.encodedPath)
        assertEquals("vpn", requests.single().url.parameters["product"])
    }

    @Test
    fun `anything but a clear contact answer means the payment page`() = runTest {
        val answers = listOf(
            """{"plans":[],"purchase":{"mode":"PROVIDER"}}""",
            """{"plans":[]}""",
            """{"plans":[],"purchase":{"mode":"CONTACT"}}""",
            """{"plans":[],"purchase":{"mode":"CONTACT","contactUrl":"http://t.me/x"}}""",
        )
        for (answer in answers) {
            val repository = repository { respondJson(answer, HttpStatusCode.OK) }
            assertEquals(PlusPurchase.PaymentPage, repository.purchase(), answer)
        }
        assertEquals(PlusPurchase.PaymentPage, repository { throw java.io.IOException("offline") }.purchase())
    }

    @Test
    fun `the payment link is never printed`() {
        val start = PaymentStart(PAYMENT_ID, PAY_URL)
        assertFalse(start.toString().contains("robokassa"), start.toString())
        assertFalse(start.toString().contains(PAYMENT_ID), start.toString())
    }

    private fun AccountResult<*>.reason(): AccountFailure = assertIs<AccountResult.Failure>(this).reason

    private fun repository(
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ) = VaziePlusPaymentRepository(
        api = VazieApiClient.create(
            engine = MockEngine { request ->
                requests += request
                handler(request)
            },
            baseUrl = ApiBaseUrl("https://vazie.example/api/v1"),
            tokens = SessionTokenProvider { Secret.of(TOKEN) },
        ),
    )

    private fun MockRequestHandleScope.respondJson(body: String, status: HttpStatusCode) =
        respond(body, status, headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"))

    private fun MockRequestHandleScope.error(code: String, status: HttpStatusCode) = respond(
        content = """{"error":{"code":"$code","message":"a message nobody shows","requestId":"r"}}""",
        status = status,
        headers = headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"),
    )

    private companion object {
        const val TOKEN = "a-synthetic-session-token-that-opens-nothing"
        const val KEY = "a-synthetic-idempotency-key"
        const val PAYMENT_ID = "0f5e3c1a-0000-4000-8000-000000000001"
        const val PAY_URL = "https://auth.robokassa.ru/Merchant/Index.aspx?InvId=1&SignatureValue=synthetic"
    }
}
