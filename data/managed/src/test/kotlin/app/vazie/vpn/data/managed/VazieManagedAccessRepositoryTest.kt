package app.vazie.vpn.data.managed

import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenProvider
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.core.network.VazieApiClient
import app.vazie.vpn.managed.api.ManagedAccessFailure
import app.vazie.vpn.managed.api.ManagedAccessId
import app.vazie.vpn.managed.api.ManagedAccessRepository
import app.vazie.vpn.managed.api.ManagedResult
import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerCatalogueStore
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary
import app.vazie.vpn.api.UnsupportedRuntimeFeature
import app.vazie.vpn.api.VpnProfile
import app.vazie.vpn.api.XrayOutbound
import app.vazie.vpn.api.XraySecurity
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest

/** The repository against a backend that answers exactly what the real one answers. */
class VazieManagedAccessRepositoryTest {

    private val tokens = RecordingTokenStore()
    private val catalogue = RecordingCatalogue()

    // The happy path

    @Test
    fun `the catalogue arrives as servers a user can choose between`() = runTest {
        val repository = repository { respondJson(SERVERS_BODY) }

        val result = repository.servers()

        val servers = assertIs<ManagedResult.Success<List<*>>>(result).value
        assertEquals(1, servers.size)
        val amsterdam = assertIs<app.vazie.vpn.managed.api.ManagedServerSummary>(servers.single())
        assertEquals("Amsterdam", amsterdam.displayName)
        assertEquals(ManagedServerAvailability.AVAILABLE, amsterdam.availability)
    }


    /** A successful fetch is the one thing that writes the cache, and it writes all of it. */
    @Test
    fun `a successful catalogue is cached`() = runTest {
        val repository = repository { respondJson(SERVERS_BODY) }

        repository.servers()

        assertEquals(1, catalogue.replacements)
        assertEquals(listOf("Amsterdam"), catalogue.stored.map { it.displayName })
    }

    /** Nothing that fails clears it. */
    @Test
    fun `a failure leaves the last good catalogue alone`() = runTest {
        repository { respondJson(SERVERS_BODY) }.servers()
        val cached = catalogue.stored

        assertEquals(
            ManagedAccessFailure.BackendUnreachable,
            repository { throw IOException("offline") }.servers().reason(),
        )
        assertEquals(cached, catalogue.stored, "an unreachable backend emptied the cache")

        assertEquals(
            ManagedAccessFailure.Unknown,
            repository { error("SOMETHING", HttpStatusCode.InternalServerError) }.servers().reason(),
        )
        assertEquals(cached, catalogue.stored, "an HTTP failure emptied the cache")

        assertEquals(
            ManagedAccessFailure.SignInRequired,
            repository(token = null) { respondJson(SERVERS_BODY) }.servers().reason(),
        )
        assertEquals(cached, catalogue.stored, "a missing session emptied the cache")
    }

    /** A fresh catalogue replaces the old one whole, so a server the backend has stopped listing disappears
     * instead of living in a cache for ever. */
    @Test
    fun `a fresh catalogue replaces the previous one`() = runTest {
        repository { respondJson(SERVERS_BODY) }.servers()
        assertEquals(listOf("Amsterdam"), catalogue.stored.map { it.displayName })

        repository { respondJson("""{"servers":[]}""") }.servers()

        assertEquals(emptyList(), catalogue.stored, "a server the backend dropped survived in cache")
    }

    @Test
    fun `ensuring access names the server and returns a runnable profile`() = runTest {
        var seenPath: String? = null
        var seenMethod: HttpMethod? = null
        var seenBody: String? = null
        val repository = repository { request ->
            seenPath = request.url.encodedPath
            seenMethod = request.method
            seenBody = request.bodyText()
            respondJson(ACCESS_BODY, status = HttpStatusCode.Created)
        }

        val result = repository.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID))

        assertEquals(HttpMethod.Post, seenMethod)
        assertEquals("/v1/access", seenPath)
        // A preference, not a region guess: the user pressed a country and that is what is asked for.
        assertTrue(seenBody.orEmpty().contains(ManagedFixtures.SERVER_ID))

        val material = assertIs<ManagedResult.Success<*>>(result).value
        val access = assertIs<app.vazie.vpn.managed.api.ManagedConnectionMaterial>(material)
        val outbound = assertIs<XrayOutbound.Vless>(
            assertIs<VpnProfile.Xray>(access.profile).outbound,
        )
        assertEquals(ManagedFixtures.CREDENTIAL, outbound.userId.expose())
        assertIs<XraySecurity.Reality>(outbound.security)
    }

    @Test
    fun `a revocation is a delete against the access it names`() = runTest {
        var seenPath: String? = null
        var seenMethod: HttpMethod? = null
        val repository = repository { request ->
            seenPath = request.url.encodedPath
            seenMethod = request.method
            respond("", HttpStatusCode.NoContent)
        }

        val result = repository.revokeAccess(ManagedAccessId(ManagedFixtures.ACCESS_ID))

        assertIs<ManagedResult.Success<Unit>>(result)
        assertEquals(HttpMethod.Delete, seenMethod)
        assertEquals("/v1/access/${ManagedFixtures.ACCESS_ID}", seenPath)
    }

    // Failures

    @Test
    fun `a device with no session never touches the network`() = runTest {
        var called = false
        val repository = repository(token = null) {
            called = true
            respondJson(SERVERS_BODY)
        }

        assertEquals(ManagedAccessFailure.SignInRequired, repository.servers().reason())
        assertEquals(
            ManagedAccessFailure.SignInRequired,
            repository.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID)).reason(),
        )
        assertEquals(
            ManagedAccessFailure.SignInRequired,
            repository.revokeAccess(ManagedAccessId(ManagedFixtures.ACCESS_ID)).reason(),
        )
        assertFalse(called, "an unauthenticated device produced traffic to Vazie")
    }

    @Test
    fun `a rejected session is discarded, and a missing credential is not`() = runTest {
        // AUTH_REQUIRED keeps the token; AUTH_INVALID clears it.
        val invalid = repository { error("AUTH_INVALID", HttpStatusCode.Unauthorized) }
        assertEquals(ManagedAccessFailure.SessionExpired, invalid.servers().reason())
        assertTrue(tokens.cleared, "a rejected session was kept")

        val fresh = RecordingTokenStore()
        val required = repository(store = fresh) { error("AUTH_REQUIRED", HttpStatusCode.Unauthorized) }
        assertEquals(ManagedAccessFailure.SignInRequired, required.servers().reason())
        assertFalse(fresh.cleared, "a working session was discarded on the wrong signal")
    }

    @Test
    fun `an unentitled account is told about the entitlement, not about a server`() = runTest {
        val repository = repository { error("SUBSCRIPTION_REQUIRED", HttpStatusCode.Forbidden) }

        assertEquals(
            ManagedAccessFailure.EntitlementMissing,
            repository.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID)).reason(),
        )
    }

    @Test
    fun `nowhere to put this account and a failed attempt are different answers`() = runTest {
        val unavailable = repository { error("SERVER_UNAVAILABLE", HttpStatusCode.ServiceUnavailable) }
        assertEquals(
            ManagedAccessFailure.ServerUnavailable,
            unavailable.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID)).reason(),
        )

        val failed = repository { error("PROVISIONING_FAILED", HttpStatusCode.BadGateway) }
        assertEquals(
            ManagedAccessFailure.ProvisioningFailed,
            failed.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID)).reason(),
        )
    }

    @Test
    fun `a rate limit carries the server's own answer about when to come back`() = runTest {
        val repository = repository {
            respond(
                content = """{"error":{"code":"RATE_LIMITED","message":"x","requestId":"r"}}""",
                status = HttpStatusCode.TooManyRequests,
                headers = headersOf("Retry-After", "42"),
            )
        }

        val reason = repository.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID)).reason()

        assertEquals(ManagedAccessFailure.RateLimited(42L), reason)
    }

    @Test
    fun `nothing answering is unreachable`() = runTest {
        val repository = repository { throw IOException("no route to host") }

        assertEquals(ManagedAccessFailure.BackendUnreachable, repository.servers().reason())
    }

    @Test
    fun `a gateway that cannot reach the backend reads as the backend being unreachable`() = runTest {
        // The bring-up case: the public proxy answers, the route to the backend does not exist yet,
        // and there is no error envelope because nothing of Vazie's produced the response.
        listOf(HttpStatusCode.BadGateway, HttpStatusCode.ServiceUnavailable, HttpStatusCode.GatewayTimeout)
            .forEach { status ->
                val repository = repository { respond("<html>nginx</html>", status) }
                assertEquals(
                    ManagedAccessFailure.BackendUnreachable,
                    repository.servers().reason(),
                    "status $status",
                )
            }
    }

    @Test
    fun `a profile this build cannot run is named as such`() = runTest {
        val repository = repository { respondJson(ACCESS_BODY_WITH_UNKNOWN_FLOW, HttpStatusCode.OK) }

        val reason = repository.ensureAccess(ManagedServerId(ManagedFixtures.SERVER_ID)).reason()

        assertEquals(
            ManagedAccessFailure.UnsupportedProfile(UnsupportedRuntimeFeature.FLOW),
            reason,
        )
    }

    @Test
    fun `revoking something that is already gone is success, not a failure`() = runTest {
        // Revocation is idempotent by contract, and "it is not there" is the state that was asked
        // for. Reporting a failure would make a second Revoke look broken while doing its job.
        val repository = repository {
            error("VPN_ACCESS_NOT_FOUND", HttpStatusCode.NotFound)
        }

        assertIs<ManagedResult.Success<Unit>>(
            repository.revokeAccess(ManagedAccessId(ManagedFixtures.ACCESS_ID)),
        )
    }

    @Test
    fun `an identifier that is not path-safe never reaches a URL`() = runTest {
        var called = false
        val repository = repository {
            called = true
            respond("", HttpStatusCode.NoContent)
        }

        val result = repository.revokeAccess(ManagedAccessId("../../internal/v1/servers"))

        assertEquals(ManagedAccessFailure.Unknown, result.reason())
        assertFalse(called, "a crafted identifier produced a request")
    }

    @Test
    fun `a catalogue entry this build cannot represent does not hide the rest`() = runTest {
        val repository = repository { respondJson(SERVERS_BODY_WITH_A_BAD_ENTRY) }

        val servers = assertIs<ManagedResult.Success<List<*>>>(repository.servers()).value

        assertEquals(1, servers.size)
    }

    @Test
    fun `no failure can print the session token`() = runTest {
        val repository = repository { error("AUTH_INVALID", HttpStatusCode.Unauthorized) }

        val result = repository.servers()

        assertFalse(result.toString().contains(TOKEN))
    }

    // -----------------------------------------------------------------------------------------

    /** The cache the repository writes through, recorded rather than mocked. */
    private class RecordingCatalogue : ManagedServerCatalogueStore {
        var replacements = 0
        var stored: List<ManagedServerSummary> = emptyList()

        override fun observe(): Flow<List<ManagedServerSummary>> = MutableStateFlow(stored)
        override suspend fun catalogue(): List<ManagedServerSummary> = stored
        override suspend fun replace(servers: List<ManagedServerSummary>) {
            replacements++
            stored = servers
        }
    }

    private fun repository(
        token: String? = TOKEN,
        store: RecordingTokenStore = tokens,
        handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData,
    ): ManagedAccessRepository {
        store.token = token
        return VazieManagedAccessRepository(
            api = VazieApiClient.create(
                engine = MockEngine { request -> handler(request) },
                baseUrl = ApiBaseUrl("https://vpn.vazie.example/v1"),
                tokens = store,
            ),
            tokens = store,
            catalogue = catalogue,
        )
    }

    private fun MockRequestHandleScope.respondJson(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ) = respond(body, status, headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"))

    private fun MockRequestHandleScope.error(code: String, status: HttpStatusCode) = respond(
        content = """{"error":{"code":"$code","message":"a message nobody shows","requestId":"r"}}""",
        status = status,
        headers = headersOf(VazieApiClient.REQUEST_ID_HEADER, "a-request-id"),
    )

    private fun ManagedResult<*>.reason(): ManagedAccessFailure =
        assertIs<ManagedResult.Failure>(this).reason

    private suspend fun HttpRequestData.bodyText(): String =
        (body as? io.ktor.http.content.TextContent)?.text.orEmpty()

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

        val SERVERS_BODY = """
            {"servers":[{"id":"${ManagedFixtures.SERVER_ID}","displayName":"Amsterdam",
             "regionId":"nl-ams","countryCode":"NL","city":"Amsterdam","status":"AVAILABLE",
             "protocols":["VLESS"]}]}
        """.trimIndent()

        val SERVERS_BODY_WITH_A_BAD_ENTRY = """
            {"servers":[{"id":"","displayName":"Nowhere","regionId":"xx","countryCode":"XX",
             "status":"AVAILABLE","protocols":["VLESS"]},
             {"id":"${ManagedFixtures.SERVER_ID}","displayName":"Amsterdam","regionId":"nl-ams",
              "countryCode":"NL","status":"AVAILABLE","protocols":["VLESS"]}]}
        """.trimIndent()

        val ACCESS_BODY = accessBody(flow = "xtls-rprx-vision")

        val ACCESS_BODY_WITH_UNKNOWN_FLOW = accessBody(flow = "xtls-rprx-direct")

        fun accessBody(flow: String) = """
            {"id":"${ManagedFixtures.ACCESS_ID}","state":"ACTIVE","createdAt":"${ManagedFixtures.CREATED_AT}",
             "server":{"id":"${ManagedFixtures.SERVER_ID}","displayName":"Amsterdam","regionId":"nl-ams",
                       "countryCode":"NL","city":"Amsterdam","status":"AVAILABLE","protocols":["VLESS"]},
             "profile":{"protocol":"VLESS","endpoint":{"host":"${ManagedFixtures.HOST}","port":2053},
                        "credential":"${ManagedFixtures.CREDENTIAL}","flow":"$flow",
                        "security":{"kind":"REALITY","serverName":"${ManagedFixtures.SERVER_NAME}",
                                    "fingerprint":"chrome","publicKey":"${ManagedFixtures.PUBLIC_KEY}",
                                    "shortId":"${ManagedFixtures.SHORT_ID}"},
                        "transport":{"kind":"TCP"}}}
        """.trimIndent()
    }
}
