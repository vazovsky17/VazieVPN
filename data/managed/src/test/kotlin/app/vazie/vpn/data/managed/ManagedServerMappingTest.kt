package app.vazie.vpn.data.managed

import app.vazie.vpn.managed.api.ManagedServerAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ManagedServerMappingTest {

    @Test
    fun `the catalogue entry keeps what a user chooses by`() {
        val server = assertNotNull(ManagedProfileMapper.mapServer(ManagedFixtures.server()))

        assertEquals(ManagedFixtures.SERVER_ID, server.id.value)
        assertEquals("Amsterdam", server.displayName)
        assertEquals("nl-ams", server.regionId)
        assertEquals("NL", server.countryCode)
        assertEquals("Amsterdam", server.city)
        assertEquals(ManagedServerAvailability.AVAILABLE, server.availability)
    }

    @Test
    fun `draining stays its own answer`() {
        // A draining server is reachable and not taking new access, so an existing access keeps
        // working. Collapsing it into unavailable would hide a server that is carrying traffic.
        val server = assertNotNull(ManagedProfileMapper.mapServer(ManagedFixtures.server(status = "DRAINING")))

        assertEquals(ManagedServerAvailability.DRAINING, server.availability)
        assertEquals(false, server.availability.acceptsNewAccess)
    }

    @Test
    fun `a status this build does not know reads as unavailable`() {
        listOf("MAINTENANCE", "", "available-ish").forEach { status ->
            val server = assertNotNull(ManagedProfileMapper.mapServer(ManagedFixtures.server(status = status)))
            assertEquals(ManagedServerAvailability.UNAVAILABLE, server.availability, "status $status")
        }
    }

    @Test
    fun `a server that does not speak VLESS is unavailable rather than a row that fails when pressed`() {
        val server = assertNotNull(
            ManagedProfileMapper.mapServer(ManagedFixtures.server(protocols = listOf("WIREGUARD"))),
        )

        assertEquals(ManagedServerAvailability.UNAVAILABLE, server.availability)
    }

    @Test
    fun `an empty protocol list is not read as a refusal`() {
        // Absent is not the same as "speaks nothing"; the backend always sends the list, and a build
        // that treated an omission as an exclusion would hide every server if it ever stopped.
        val server = assertNotNull(
            ManagedProfileMapper.mapServer(ManagedFixtures.server(protocols = emptyList())),
        )

        assertEquals(ManagedServerAvailability.AVAILABLE, server.availability)
    }

    @Test
    fun `a server with no identifier is not a server`() {
        assertNull(ManagedProfileMapper.mapServer(ManagedFixtures.server().copy(id = "")))
    }
}
