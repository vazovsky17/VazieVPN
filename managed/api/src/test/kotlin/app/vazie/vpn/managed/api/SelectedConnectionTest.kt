package app.vazie.vpn.managed.api

import app.vazie.vpn.core.model.ProfileId
import kotlin.test.Test
import kotlin.test.assertEquals

/** The rule Home, the widgets and the Quick Settings tile all connect by. */
class SelectedConnectionTest {

    @Test
    fun `a chosen Vazie server is what Connect means`() {
        assertEquals(
            SelectedConnection.Managed(AMSTERDAM),
            selectedConnection(managed = AMSTERDAM, custom = null),
        )
    }

    @Test
    fun `a chosen configuration is what Connect means`() {
        assertEquals(
            SelectedConnection.Custom(HOME),
            selectedConnection(managed = null, custom = HOME),
        )
    }

    @Test
    fun `nothing chosen means nothing`() {
        assertEquals(SelectedConnection.None, selectedConnection(managed = null, custom = null))
    }

    /** If both are set despite the invariant, the Vazie server wins everywhere. */
    @Test
    fun `with both set the Vazie server wins`() {
        assertEquals(
            SelectedConnection.Managed(AMSTERDAM),
            selectedConnection(managed = AMSTERDAM, custom = HOME),
        )
    }

    private companion object {
        val AMSTERDAM = SelectedManagedServer(
            id = ManagedServerId("nl-1"),
            displayName = "Amsterdam",
            countryCode = "NL",
        )
        val HOME = ProfileId("home-relay")
    }
}
