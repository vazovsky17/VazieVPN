package app.vazie.vpn.data.managed

import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** What may be written into a file the home screen reads. */
class ManagedCatalogueCarriesNoSecretsTest {

    private val directory = File.createTempFile("vazie-managed", "").let {
        it.delete()
        it.mkdirs()
        it
    }
    private val file = File(directory, "server-catalogue")

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `the cached model has no field a secret could sit in`() {
        val offenders = ManagedServerSummary::class.java.declaredFields
            .map { it.name }
            .filterNot { it.startsWith("$") }
            .filter { name -> FORBIDDEN.any { name.contains(it, ignoreCase = true) } }

        assertTrue(offenders.isEmpty(), "the cached server model names a secret: $offenders")
    }

    @Test
    fun `the cached model carries only what the catalogue is allowed to publish`() {
        val fields = ManagedServerSummary::class.java.declaredFields
            .map { it.name }
            .filterNot { it.startsWith("$") }
            .toSet()

        assertTrue(
            fields == ALLOWED,
            "the cached server model has grown beyond the public catalogue: ${fields - ALLOWED}",
        )
    }

    @Test
    fun `nothing written to the file is named after a secret`() = runTest {
        FileManagedServerCatalogue(file).replace(listOf(AMSTERDAM))

        val written = file.readText()
        val offenders = FORBIDDEN.filter { written.contains(it, ignoreCase = true) }

        assertTrue(offenders.isEmpty(), "the cache file contains $offenders")
    }

    /** A response with fields this client has never heard of - including one that is plainly a secret - is
     * mapped through, and nothing but the six safe values reaches the file. */
    @Test
    fun `a server's address is kept in memory and never reaches the cache`() = runTest {
        val response = VazieApiClientJson.decodeFromString(
            ServersResponseDto.serializer(),
            """
            {"servers":[{
              "id":"nl-1","displayName":"Amsterdam","regionId":"eu-west","countryCode":"NL",
              "status":"AVAILABLE","protocols":["VLESS"],"endpoint":{"host":"203.0.113.77","port":443}
            }]}
            """.trimIndent(),
        )
        val memory = ServerEndpointMemory()

        FileManagedServerCatalogue(file).replace(response.servers.mapNotNull(ManagedProfileMapper::mapServer))
        memory.remember(response.servers)

        assertTrue(!file.readText().contains("203.0.113.77"), "a server address reached the cache file")
        assertTrue(memory.current.value["nl-1"]?.port == 443, "the address was not kept for the latency check")
    }

    @Test
    fun `a field the backend adds later cannot reach the cache`() = runTest {
        val fromTheFuture = VazieApiClientJson.decodeFromString(
            ServersResponseDto.serializer(),
            """
            {"servers":[{
              "id":"nl-1","displayName":"Amsterdam","regionId":"eu-west","countryCode":"NL",
              "city":"Amsterdam","status":"AVAILABLE","protocols":["VLESS"],
              "credential":"$LEAKED","managementEndpoint":"10.0.0.1:9000",
              "publicKey":"$LEAKED","shortId":"$LEAKED"
            }]}
            """.trimIndent(),
        )

        FileManagedServerCatalogue(file).replace(
            fromTheFuture.servers.mapNotNull(ManagedProfileMapper::mapServer),
        )

        val written = file.readText()
        assertTrue(!written.contains(LEAKED), "a future field reached the cache: $written")
        assertTrue(!written.contains("managementEndpoint"), "a management field reached the cache")
        assertTrue(written.contains("Amsterdam"), "the safe fields did not survive")
    }

    private companion object {
        /** A synthetic value shaped like the thing that must never arrive here. */
        const val LEAKED = "00000000-0000-4000-8000-000000000042"

        val ALLOWED = setOf(
            "id",
            "displayName",
            "regionId",
            "countryCode",
            "city",
            "availability",
        )

        val FORBIDDEN = listOf(
            "credential",
            "token",
            "secret",
            "publicKey",
            "shortId",
            "endpoint",
            "password",
            "privateKey",
            "managementEndpoint",
        )

        val AMSTERDAM = ManagedServerSummary(
            id = ManagedServerId("nl-1"),
            displayName = "Amsterdam",
            regionId = "eu-west",
            countryCode = "NL",
            city = "Amsterdam",
            availability = ManagedServerAvailability.AVAILABLE,
        )
    }
}

private val VazieApiClientJson = app.vazie.vpn.core.network.VazieApiClient.json
