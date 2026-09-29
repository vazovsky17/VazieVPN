package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.Account
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.EmailCodeRequested
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenStore
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
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class VazieAccountRepositoryTest {

    private val tokens = RecordingTokenStore()
    private val requests = mutableListOf<Recorded>()

    // ---------------------------------------------------------------- start

    @Test
    fun `requesting a code posts the address and returns when to offer resend`() = runTest {
        val repository = repository(token = null) { respondJson("""{"resendAfterSeconds":60}""", HttpStatusCode.Accepted) }

        val result = repository.requestEmailCode("  person@example.com ")

        assertEquals(AccountResult.Success(EmailCodeRequested(60)), result)
        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("/api/v1/auth/email/start", request.path)
        assertEquals("person@example.com", request.json("email"))
    }

    @Test
    fun `an address the backend refuses is an invalid email`() = runTest {
        val repository = repository(token = null) { error("EMAIL_INVALID", HttpStatusCode.BadRequest) }

        assertEquals(AccountFailure.InvalidEmail, repository.requestEmailCode("nope").reason())
    }

    @Test
    fun `asking again too soon is rate limited with the backend's retry-after`() = runTest {
        val repository = repository(token = null) {
            error("RATE_LIMITED", HttpStatusCode.TooManyRequests, retryAfter = "42")
        }

        assertEquals(
            AccountFailure.RateLimited(42),
            repository.requestEmailCode("person@example.com").reason(),
        )
    }

    @Test
    fun `a backend that cannot send mail says so`() = runTest {
        val repository = repository(token = null) { error("EMAIL_UNAVAILABLE", HttpStatusCode.ServiceUnavailable) }

        assertEquals(
            AccountFailure.EmailUnavailable,
            repository.requestEmailCode("person@example.com").reason(),
        )
    }

    @Test
    fun `sign-in switched off on the backend is not enabled`() = runTest {
        val repository = repository(token = null) { error("NOT_ENABLED", HttpStatusCode.NotFound) }

        assertEquals(AccountFailure.NotEnabled, repository.requestEmailCode("person@example.com").reason())
    }

    @Test
    fun `no network while requesting a code is unreachable`() = runTest {
        val repository = repository(token = null) { throw IOException("offline") }

        assertEquals(AccountFailure.Unreachable, repository.requestEmailCode("person@example.com").reason())
    }

    // ---------------------------------------------------------------- verify

    @Test
    fun `a correct code stores the session and loads the account`() = runTest {
        val repository = repository(token = null) { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/email/verify" -> respondJson(VERIFY_BODY)
                "/api/v1/me" -> respondJson(ME_BODY)
                else -> error("NOT_FOUND", HttpStatusCode.NotFound)
            }
        }

        val result = repository.verifyEmailCode("person@example.com", Secret.of("123456"))

        val account = assertIs<AccountResult.Success<Account>>(result).value
        assertEquals(ACCOUNT_ID, account.id)
        assertEquals("person@example.com", account.email)
        assertEquals("ACTIVE", account.status)
        assertEquals(emptyList(), account.entitlements)
        assertEquals(TOKEN, tokens.token)
        assertEquals(AccountState.SignedIn(account), repository.state.value)

        val verify = requests.first()
        assertEquals("/api/v1/auth/email/verify", verify.path)
        assertEquals("person@example.com", verify.json("email"))
        assertEquals("123456", verify.json("code"))
        assertEquals("ANDROID", verify.json("platform"))
        assertEquals("Google Pixel 8", verify.json("deviceName"))
        assertNull(verify.authorization, "verify must not carry a session it has not been given yet")
        assertEquals("Bearer $TOKEN", requests[1].authorization)
    }

    @Test
    fun `a wrong code stores nothing`() = runTest {
        val repository = repository(token = null) { error("EMAIL_CODE_INVALID", HttpStatusCode.BadRequest) }

        val result = repository.verifyEmailCode("person@example.com", Secret.of("000000"))

        assertEquals(AccountFailure.InvalidCode, result.reason())
        assertNull(tokens.token)
        assertEquals(1, requests.size, "a failed verify must not go on to /me")
    }

    @Test
    fun `an expired or exhausted code is the same invalid code`() = runTest {
        // The backend answers wrong, expired and exhausted with one error, on purpose.
        val repository = repository(token = null) { error("EMAIL_CODE_INVALID", HttpStatusCode.BadRequest) }

        assertEquals(
            AccountFailure.InvalidCode,
            repository.verifyEmailCode("person@example.com", Secret.of("654321")).reason(),
        )
    }

    @Test
    fun `verify rate limited keeps nothing`() = runTest {
        val repository = repository(token = null) {
            error("RATE_LIMITED", HttpStatusCode.TooManyRequests, retryAfter = "30")
        }

        assertEquals(
            AccountFailure.RateLimited(30),
            repository.verifyEmailCode("person@example.com", Secret.of("123456")).reason(),
        )
        assertNull(tokens.token)
    }

    @Test
    fun `no network while verifying is unreachable and stores nothing`() = runTest {
        val repository = repository(token = null) { throw IOException("offline") }

        assertEquals(
            AccountFailure.Unreachable,
            repository.verifyEmailCode("person@example.com", Secret.of("123456")).reason(),
        )
        assertNull(tokens.token)
    }

    @Test
    fun `a sign-in whose me cannot be reached is still signed in, and offline`() = runTest {
        val repository = repository(token = null) { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/email/verify" -> respondJson(VERIFY_BODY)
                else -> throw IOException("offline")
            }
        }

        val account = assertIs<AccountResult.Success<Account>>(
            repository.verifyEmailCode("person@example.com", Secret.of("123456")),
        ).value

        assertEquals("person@example.com", account.email)
        assertEquals(TOKEN, tokens.token)
        assertIs<AccountState.Offline>(repository.state.value)
    }

    // ---------------------------------------------------------------- /me and cold start

    @Test
    fun `no stored session is signed out without asking the backend`() = runTest {
        val repository = repository(token = null) { error("UNEXPECTED", HttpStatusCode.InternalServerError) }

        assertEquals(AccountState.SignedOut, repository.refresh())
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `a stored session the backend accepts is signed in`() = runTest {
        val repository = repository { respondJson(ME_BODY) }

        val state = assertIs<AccountState.SignedIn>(repository.refresh())

        assertEquals(ACCOUNT_ID, state.account.id)
        assertEquals("person@example.com", state.account.email)
        assertEquals("/api/v1/me", requests.single().path)
        assertEquals("Bearer $TOKEN", requests.single().authorization)
        assertEquals(TOKEN, tokens.token)
    }

    @Test
    fun `VPN Plus comes from the managed-server entitlement`() = runTest {
        val repository = repository {
            respondJson(
                ME_BODY.replace(
                    "\"entitlements\":[]",
                    "\"entitlements\":[{\"key\":\"MANAGED_SERVER_ACCESS\",\"validUntil\":\"2027-09-26T00:00:00Z\"}]",
                ),
            )
        }

        val state = assertIs<AccountState.SignedIn>(repository.refresh())

        assertEquals(PlusAccess("2027-09-26T00:00:00Z"), state.account.plus)
    }

    @Test
    fun `no managed-server entitlement is no VPN Plus`() = runTest {
        val repository = repository { respondJson(ME_BODY) }

        assertNull(assertIs<AccountState.SignedIn>(repository.refresh()).account.plus)
    }

    @Test
    fun `a stored session the backend rejects is forgotten`() = runTest {
        val repository = repository { error("AUTH_INVALID", HttpStatusCode.Unauthorized) }

        assertEquals(AccountState.SignedOut, repository.refresh())
        assertTrue(tokens.cleared)
        assertNull(tokens.token)
    }

    @Test
    fun `a bare 401 without a code is a rejected session too`() = runTest {
        val repository = repository { respond("", HttpStatusCode.Unauthorized) }

        assertEquals(AccountState.SignedOut, repository.refresh())
        assertNull(tokens.token)
    }

    @Test
    fun `no network keeps the session and reports offline`() = runTest {
        val repository = repository { throw IOException("offline") }

        assertEquals(AccountState.Offline(null), repository.refresh())
        assertEquals(TOKEN, tokens.token)
        assertFalse(tokens.cleared)
    }

    @Test
    fun `a gateway or server error keeps the session too`() = runTest {
        listOf(HttpStatusCode.BadGateway, HttpStatusCode.InternalServerError).forEach { status ->
            val store = RecordingTokenStore()
            val repository = repository(store = store) { respond("<html>", status) }

            assertIs<AccountState.Offline>(repository.refresh())
            assertEquals(TOKEN, store.token, "a $status cleared the session")
        }
    }

    @Test
    fun `offline after a sign-in remembers the account this process knew`() = runTest {
        var online = true
        val repository = repository { if (online) respondJson(ME_BODY) else throw IOException("offline") }
        val known = assertIs<AccountState.SignedIn>(repository.refresh()).account

        online = false

        assertEquals(AccountState.Offline(known), repository.refresh())
    }

    // ---------------------------------------------------------------- sign-out

    @Test
    fun `signing out ends the session on the backend and forgets it here`() = runTest {
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        assertEquals(AccountResult.Success(Unit), repository.signOut())

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/session", request.path)
        assertEquals("Bearer $TOKEN", request.authorization)
        assertNull(tokens.token)
        assertEquals(AccountState.SignedOut, repository.state.value)
    }

    @Test
    fun `signing out a session the backend already forgot still signs out`() = runTest {
        val repository = repository { error("AUTH_INVALID", HttpStatusCode.Unauthorized) }

        assertEquals(AccountResult.Success(Unit), repository.signOut())
        assertNull(tokens.token)
        assertEquals(AccountState.SignedOut, repository.state.value)
    }

    @Test
    fun `signing out without a network keeps the session until the user decides`() = runTest {
        val repository = repository { throw IOException("offline") }

        assertEquals(AccountFailure.Unreachable, repository.signOut().reason())
        assertEquals(TOKEN, tokens.token)
    }

    @Test
    fun `signing out locally forgets the session without the backend`() = runTest {
        val repository = repository { error("UNEXPECTED", HttpStatusCode.InternalServerError) }

        repository.signOutLocally()

        assertNull(tokens.token)
        assertEquals(AccountState.SignedOut, repository.state.value)
        assertTrue(requests.isEmpty())
    }

    // ---------------------------------------------------------------- account deletion

    @Test
    fun `deleting sends the confirmation and forgets the session`() = runTest {
        val repository = repository { respond("", HttpStatusCode.NoContent) }

        assertEquals(AccountResult.Success(Unit), repository.deleteAccount())

        val request = requests.single()
        assertEquals(HttpMethod.Delete, request.method)
        assertEquals("/api/v1/account", request.path)
        assertEquals("Bearer $TOKEN", request.authorization)
        assertEquals("DELETE", Json.parseToJsonElement(request.body).jsonObject["confirm"]?.jsonPrimitive?.content)
        assertNull(tokens.token)
        assertEquals(AccountState.SignedOut, repository.state.value)
    }

    @Test
    fun `a deletion the backend never heard keeps the session`() = runTest {
        val repository = repository { throw IOException("offline") }

        assertEquals(AccountFailure.Unreachable, repository.deleteAccount().reason())
        assertEquals(TOKEN, tokens.token)
    }

    // ---------------------------------------------------------------- secrets

    @Test
    fun `nothing the repository returns can print the token, the code or the address`() = runTest {
        val repository = repository(token = null) { request ->
            when (request.url.encodedPath) {
                "/api/v1/auth/email/verify" -> respondJson(VERIFY_BODY)
                else -> respondJson(ME_BODY)
            }
        }

        val rendered = listOf(
            repository.verifyEmailCode("person@example.com", Secret.of("123456")).toString(),
            repository.state.value.toString(),
            EmailVerifyRequestDto("person@example.com", "123456", "ANDROID").toString(),
            EmailStartRequestDto("person@example.com").toString(),
            Json.decodeFromString(EmailVerifyResponseDto.serializer(), VERIFY_BODY).toString(),
            Json { ignoreUnknownKeys = true }.decodeFromString(MeResponseDto.serializer(), ME_BODY).toString(),
        )

        rendered.forEach { text ->
            assertFalse(text.contains(TOKEN), "a token in: $text")
            assertFalse(text.contains("123456"), "a code in: $text")
            assertFalse(text.contains("person@example.com"), "an address in: $text")
            assertFalse(text.contains(ACCOUNT_ID), "a full account id in: $text")
        }
    }

    @Test
    fun `a device name is manufacturer and model, and nothing else`() {
        assertEquals("Google Pixel 8", AccountStorage.deviceName("Google", "Pixel 8"))
        assertEquals("Samsung SM-S918B", AccountStorage.deviceName("samsung", "SM-S918B"))
        assertEquals("Google Pixel 8", AccountStorage.deviceName("google", "Pixel 8"))
        assertEquals("OnePlus A6013", AccountStorage.deviceName("OnePlus", "OnePlus A6013"))
        assertEquals("OnePlus", AccountStorage.deviceName("OnePlus", ""))
        assertNull(AccountStorage.deviceName(null, null))
        assertEquals(64, AccountStorage.deviceName("M", "x".repeat(200))?.length)
    }

    // ---------------------------------------------------------------- plumbing

    private fun repository(
        token: String? = TOKEN,
        store: RecordingTokenStore = tokens,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): VazieAccountRepository {
        store.token = token
        return VazieAccountRepository(
            api = VazieApiClient.create(
                engine = MockEngine { request ->
                    requests += Recorded(
                        method = request.method,
                        path = request.url.encodedPath,
                        authorization = request.headers[HttpHeaders.Authorization],
                        body = (request.body as? io.ktor.http.content.TextContent)?.text.orEmpty(),
                    )
                    handler(request)
                },
                baseUrl = ApiBaseUrl("https://vazie.example/api/v1"),
                tokens = store,
            ),
            tokens = store,
            deviceName = "Google Pixel 8",
        )
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"))

    private fun MockRequestHandleScope.error(
        code: String,
        status: HttpStatusCode,
        retryAfter: String? = null,
    ) = respond(
        content = """{"error":{"code":"$code","message":"a message nobody shows","requestId":"r"}}""",
        status = status,
        headers = if (retryAfter == null) {
            headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id")
        } else {
            io.ktor.http.headers {
                append(VazieApiClient.REQUEST_ID_HEADER, "a-request-id")
                append(HttpHeaders.RetryAfter, retryAfter)
            }
        },
    )

    private fun AccountResult<*>.reason(): AccountFailure = assertIs<AccountResult.Failure>(this).reason

    private data class Recorded(
        val method: HttpMethod,
        val path: String,
        val authorization: String?,
        val body: String,
    ) {
        fun json(key: String): String? =
            Json.parseToJsonElement(body).jsonObject[key]?.jsonPrimitive?.content
    }

    private class RecordingTokenStore : SessionTokenStore {
        var token: String? = TOKEN
        var cleared: Boolean = false

        override suspend fun token(): Secret<String>? = token?.let { Secret.of(it) }

        override suspend fun store(token: Secret<String>) {
            this.token = token.expose()
        }

        override suspend fun clear() {
            cleared = true
            token = null
        }
    }

    private companion object {
        const val TOKEN = "a-synthetic-session-token-that-opens-nothing"
        const val ACCOUNT_ID = "00000000-0000-4000-8000-00000000abcd"

        val VERIFY_BODY = """
            {"accountId":"$ACCOUNT_ID","deviceId":"00000000-0000-4000-8000-00000000d001",
             "token":"$TOKEN","expiresAt":"2026-10-25T00:00:00Z","email":"person@example.com",
             "accountCreated":false}
        """.trimIndent()

        val ME_BODY = """
            {"accountId":"$ACCOUNT_ID","status":"ACTIVE","createdAt":"2026-09-25T00:00:00Z",
             "email":"person@example.com",
             "device":{"id":"00000000-0000-4000-8000-00000000d001","platform":"ANDROID",
                       "displayName":"Google Pixel 8","createdAt":"2026-09-25T00:00:00Z"},
             "entitlements":[]}
        """.trimIndent()
    }
}
