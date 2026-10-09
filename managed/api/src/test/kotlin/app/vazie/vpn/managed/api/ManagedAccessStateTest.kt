package app.vazie.vpn.managed.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManagedAccessStateTest {

    @Test
    fun `only an active access can carry a connection`() {
        val usable = ManagedAccessState.entries.filter { it.isUsable }

        assertEquals(listOf(ManagedAccessState.ACTIVE), usable)
    }

    @Test
    fun `the state set does not contain a revoked state`() {
        // No REVOKED state: the backend hides revoked records, so a client only sees an absence.
        assertTrue(ManagedAccessState.entries.none { it.name == "REVOKED" })
    }

    @Test
    fun `only an available server is worth asking for new access`() {
        assertEquals(
            listOf(ManagedServerAvailability.AVAILABLE),
            ManagedServerAvailability.entries.filter { it.acceptsNewAccess },
        )
    }
}
