package app.vazie.vpn.data.managed

import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/** The catalogue cache as a file, checked the way a widget will meet it: through a second instance, after the
 * process that wrote it is gone. */
class FileManagedServerCatalogueTest {

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

    /** The point of the whole thing: a widget started by the launcher after a reboot reads what the app wrote
     * before it died. */
    @Test
    fun `a catalogue survives a new instance`() = runTest {
        FileManagedServerCatalogue(file).replace(listOf(AMSTERDAM, FRANKFURT))

        assertEquals(
            listOf(AMSTERDAM, FRANKFURT),
            FileManagedServerCatalogue(file).catalogue(),
        )
    }

    @Test
    fun `every safe field survives the round trip`() = runTest {
        FileManagedServerCatalogue(file).replace(listOf(AMSTERDAM))

        val restored = FileManagedServerCatalogue(file).catalogue().single()
        assertEquals(AMSTERDAM.id, restored.id)
        assertEquals(AMSTERDAM.displayName, restored.displayName)
        assertEquals(AMSTERDAM.regionId, restored.regionId)
        assertEquals(AMSTERDAM.countryCode, restored.countryCode)
        assertEquals(AMSTERDAM.city, restored.city)
        assertEquals(AMSTERDAM.availability, restored.availability)
    }

    @Test
    fun `no catalogue is an empty one, not an error`() = runTest {
        assertEquals(emptyList(), FileManagedServerCatalogue(file).catalogue())
    }

    /** A file that no longer parses is read as no catalogue. */
    @Test
    fun `a corrupt catalogue reads as empty rather than throwing`() = runTest {
        file.parentFile?.mkdirs()
        file.writeText("{ this is not json")

        assertEquals(emptyList(), FileManagedServerCatalogue(file).catalogue())
    }

    @Test
    fun `a truncated catalogue reads as empty rather than throwing`() = runTest {
        FileManagedServerCatalogue(file).replace(listOf(AMSTERDAM, FRANKFURT))
        file.writeText(file.readText().take(HALF_A_FILE))

        assertEquals(emptyList(), FileManagedServerCatalogue(file).catalogue())
    }

    /** A replacement is a replacement: what the backend stopped listing stops being cached. */
    @Test
    fun `a replacement drops what is no longer listed`() = runTest {
        val store = FileManagedServerCatalogue(file)
        store.replace(listOf(AMSTERDAM, FRANKFURT))

        store.replace(listOf(FRANKFURT))

        assertEquals(listOf(FRANKFURT), FileManagedServerCatalogue(file).catalogue())
    }

    @Test
    fun `an empty catalogue can replace a full one`() = runTest {
        val store = FileManagedServerCatalogue(file)
        store.replace(listOf(AMSTERDAM))

        store.replace(emptyList())

        assertEquals(emptyList(), FileManagedServerCatalogue(file).catalogue())
    }

    /** An availability the backend invents later is read as unavailable rather than as a crash or as an
     * offer. */
    @Test
    fun `an availability from the future is not an offer`() = runTest {
        file.parentFile?.mkdirs()
        file.writeText(
            """{"servers":[{"id":"nl-1","displayName":"Amsterdam","regionId":"eu-west",""" +
                """"countryCode":"NL","availability":"PROBABLY_FINE"}]}""",
        )

        val restored = FileManagedServerCatalogue(file).catalogue().single()
        assertEquals(ManagedServerAvailability.UNAVAILABLE, restored.availability)
        assertTrue(!restored.availability.acceptsNewAccess)
    }

    /** The staging file is not left behind. A directory that accumulates `.writing` files after every refresh
     * is a leak nobody notices until it is large. */
    @Test
    fun `writing leaves one file behind, not two`() = runTest {
        val store = FileManagedServerCatalogue(file)
        store.replace(listOf(AMSTERDAM))
        store.replace(listOf(FRANKFURT))

        assertEquals(
            listOf(file.name),
            directory.listFiles().orEmpty().map { it.name }.sorted(),
        )
    }

    private companion object {
        const val HALF_A_FILE = 20

        val AMSTERDAM = ManagedServerSummary(
            id = ManagedServerId("nl-1"),
            displayName = "Amsterdam",
            regionId = "eu-west",
            countryCode = "NL",
            city = "Amsterdam",
            availability = ManagedServerAvailability.AVAILABLE,
        )

        val FRANKFURT = ManagedServerSummary(
            id = ManagedServerId("de-1"),
            displayName = "Frankfurt",
            regionId = "eu-central",
            countryCode = "DE",
            city = "Frankfurt",
            availability = ManagedServerAvailability.DRAINING,
        )
    }
}
