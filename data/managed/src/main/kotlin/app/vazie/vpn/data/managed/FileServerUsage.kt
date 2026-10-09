package app.vazie.vpn.data.managed

import app.vazie.vpn.core.network.VazieApiClient
import java.io.File
import java.time.Instant
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

/** When each Vazie server last carried a tunnel on this device, in a file, in the clear. */
internal class FileServerUsage(
    private val file: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    private val mutex = Mutex()
    private val cache = MutableStateFlow<Map<String, Instant>>(emptyMap())

    /** Server id → when it was last used. */
    fun observe(): Flow<Map<String, Instant>> =
        cache.asStateFlow().onStart { mutex.withLock { cache.value = read() } }

    suspend fun markUsed(serverId: String, at: Instant) = mutex.withLock {
        val updated = read() + (serverId to at)
        withContext(dispatcher) {
            file.parentFile?.mkdirs()
            file.writeText(
                VazieApiClient.json.encodeToString(
                    StoredUsage.serializer(),
                    StoredUsage(updated.mapValues { (_, instant) -> instant.toEpochMilli() }),
                ),
            )
        }
        cache.value = updated
    }

    private suspend fun read(): Map<String, Instant> = withContext(dispatcher) {
        runCatching {
            VazieApiClient.json
                .decodeFromString(StoredUsage.serializer(), file.readText())
                .lastUsedAt
                .mapValues { (_, millis) -> Instant.ofEpochMilli(millis) }
        }.getOrDefault(emptyMap())
    }

    @Serializable
    private data class StoredUsage(val lastUsedAt: Map<String, Long>)
}
