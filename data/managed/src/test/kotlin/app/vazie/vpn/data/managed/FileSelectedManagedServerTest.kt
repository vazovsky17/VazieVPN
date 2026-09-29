package app.vazie.vpn.data.managed

import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.SelectedManagedServer
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

class FileSelectedManagedServerTest {

    private val directory = File.createTempFile("vazie-managed", "").let {
        it.delete()
        it.mkdirs()
        it
    }
    private val file = File(directory, "selected-server")

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun `a chosen server survives a new instance`() = runTest {
        FileSelectedManagedServer(file).select(AMSTERDAM)

        assertEquals(AMSTERDAM, FileSelectedManagedServer(file).selected())
    }

    @Test
    fun `nothing chosen is nothing, not an error`() = runTest {
        assertNull(FileSelectedManagedServer(file).selected())
    }

    @Test
    fun `a wiped or unreadable file reads as nothing chosen`() = runTest {
        // A preference has no useful difference between missing, blank and unreadable, and inventing
        // one would turn a cleared cache into something the user has to understand.
        file.writeText("   ")

        assertNull(FileSelectedManagedServer(file).selected())
    }

    @Test
    fun `clearing removes the choice`() = runTest {
        val store = FileSelectedManagedServer(file)
        store.select(AMSTERDAM)

        store.clear()

        assertNull(store.selected())
        assertNull(store.observeSelected().first())
    }

    @Test
    fun `observers see the current choice`() = runTest {
        val store = FileSelectedManagedServer(file)

        store.select(AMSTERDAM)

        assertEquals(AMSTERDAM, store.observeSelected().first())
    }

    private companion object {
        val AMSTERDAM = SelectedManagedServer(
            id = ManagedServerId("00000000-0000-4000-8000-0000000000a2"),
            displayName = "Amsterdam",
            countryCode = "NL",
        )
    }
}
