package app.vazie.vpn.managed.api

import app.vazie.vpn.core.model.ProfileId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ManagedIdsTest {

    @Test
    fun `a managed access names a profile id derived from itself`() {
        val access = ManagedAccessId("7f1c2a80-0000-4000-8000-000000000001")

        assertEquals(
            ProfileId("vazie-managed-7f1c2a80-0000-4000-8000-000000000001"),
            access.profileId,
        )
    }

    @Test
    fun `a managed profile id can never be spelled by an imported profile`() {
        // The prefix keeps managed ids disjoint from imported profiles' random UUIDs.
        val imported = ProfileId("7f1c2a80-0000-4000-8000-000000000001")
        val managed = ManagedAccessId("7f1c2a80-0000-4000-8000-000000000001").profileId

        assertNotEquals(imported, managed)
        assertTrue(managed.value.startsWith(ManagedAccessId.PROFILE_ID_PREFIX))
    }

    @Test
    fun `the managed namespace does not collide with the built-in one`() {
        // Must stay distinct from the retired "vazie-builtin-" prefix.
        assertTrue(ManagedAccessId.PROFILE_ID_PREFIX != "vazie-builtin-")
        assertTrue(
            !ManagedAccessId("nl-1").profileId.value.startsWith("vazie-builtin-"),
        )
    }

    @Test
    fun `a blank identifier is refused rather than carried`() {
        assertFailsWith<IllegalArgumentException> { ManagedAccessId("") }
        assertFailsWith<IllegalArgumentException> { ManagedServerId("  ") }
    }
}
