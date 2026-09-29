package app.vazie.vpn.data.managed

import app.vazie.vpn.core.network.VazieApiClient
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.SelectedManagedServer
import app.vazie.vpn.managed.api.SelectedManagedServerStore
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

/** The chosen Vazie server, in a file, in the clear. */
internal class FileSelectedManagedServer(
    private val file: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : SelectedManagedServerStore {

    private val mutex = Mutex()
    private val cache = MutableStateFlow<SelectedManagedServer?>(null)

    override fun observeSelected(): Flow<SelectedManagedServer?> =
        cache.asStateFlow().onStart { cache.value = read() }

    override suspend fun selected(): SelectedManagedServer? = mutex.withLock { read() }

    override suspend fun select(server: SelectedManagedServer) = mutex.withLock {
        withContext(dispatcher) {
            file.parentFile?.mkdirs()
            file.writeText(
                VazieApiClient.json.encodeToString(StoredSelection.serializer(), server.toRecord()),
            )
        }
        cache.value = server
    }

    override suspend fun clear() = mutex.withLock {
        withContext(dispatcher) { file.delete() }
        cache.value = null
    }

    private suspend fun read(): SelectedManagedServer? = withContext(dispatcher) {
        runCatching {
            VazieApiClient.json
                .decodeFromString(StoredSelection.serializer(), file.readText())
                .toDomain()
        }.getOrNull()
    }

    /** The on-disk shape, separate from the domain type so that renaming a field on one is not a silent
     * change to the other - the same reason the wire DTOs are separate from the model. */
    @Serializable
    private data class StoredSelection(
        val id: String,
        val displayName: String,
        val countryCode: String,
    ) {
        fun toDomain(): SelectedManagedServer? {
            if (id.isBlank()) return null
            return SelectedManagedServer(
                id = ManagedServerId(id),
                displayName = displayName,
                countryCode = countryCode,
            )
        }
    }

    private fun SelectedManagedServer.toRecord() = StoredSelection(
        id = id.value,
        displayName = displayName,
        countryCode = countryCode,
    )
}
