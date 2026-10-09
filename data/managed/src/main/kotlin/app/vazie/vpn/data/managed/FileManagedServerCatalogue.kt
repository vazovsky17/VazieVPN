package app.vazie.vpn.data.managed

import app.vazie.vpn.core.network.VazieApiClient
import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerCatalogueStore
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary
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

/** The catalogue cache, as a small JSON file beside the other managed stores. */
internal class FileManagedServerCatalogue(
    private val file: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ManagedServerCatalogueStore {

    private val mutex = Mutex()
    private val cache = MutableStateFlow<List<ManagedServerSummary>>(emptyList())

    override fun observe(): Flow<List<ManagedServerSummary>> =
        cache.asStateFlow().onStart { cache.value = read() }

    override suspend fun catalogue(): List<ManagedServerSummary> = mutex.withLock { read() }

    override suspend fun replace(servers: List<ManagedServerSummary>) = mutex.withLock {
        withContext(dispatcher) {
            runCatching {
                file.parentFile?.mkdirs()
                val staging = File(file.parentFile, file.name + STAGING_SUFFIX)
                staging.writeText(
                    VazieApiClient.json.encodeToString(
                        StoredCatalogue.serializer(),
                        StoredCatalogue(servers.map { it.toRecord() }),
                    ),
                )
                if (!staging.renameTo(file)) {
                    // A rename can fail where a copy still works; the copy is not atomic, so it is
                    // the fallback rather than the method.
                    staging.copyTo(file, overwrite = true)
                    staging.delete()
                }
            }
        }
        cache.value = servers
    }

    private suspend fun read(): List<ManagedServerSummary> = withContext(dispatcher) {
        runCatching {
            VazieApiClient.json
                .decodeFromString(StoredCatalogue.serializer(), file.readText())
                .servers
                .mapNotNull { it.toDomain() }
        }.getOrElse { emptyList() }
    }

    /** The cached shape, written out field by field. */
    @Serializable
    private data class StoredCatalogue(val servers: List<StoredServer> = emptyList())

    @Serializable
    private data class StoredServer(
        val id: String,
        val displayName: String,
        val regionId: String,
        val countryCode: String,
        val city: String? = null,
        val availability: String,
    ) {
        fun toDomain(): ManagedServerSummary? {
            if (id.isBlank()) return null
            val known = ManagedServerAvailability.entries
                .firstOrNull { it.name == availability }
                ?: ManagedServerAvailability.UNAVAILABLE
            return ManagedServerSummary(
                id = ManagedServerId(id),
                displayName = displayName,
                regionId = regionId,
                countryCode = countryCode,
                city = city,
                availability = known,
            )
        }
    }

    private fun ManagedServerSummary.toRecord() = StoredServer(
        id = id.value,
        displayName = displayName,
        regionId = regionId,
        countryCode = countryCode,
        city = city,
        availability = availability.name,
    )

    private companion object {
        const val STAGING_SUFFIX = ".writing"
    }
}
