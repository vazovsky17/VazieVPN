package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PlusCatalog
import app.vazie.vpn.account.api.PlusCatalogRepository
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.core.network.ApiResult
import app.vazie.vpn.core.network.VazieApiClient
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/** The VPN Plus plans from `GET /plans?product=vpn`, public and read without a session. A good answer is kept in
 * [store], so the plans can still be shown — marked stale — when the backend cannot be reached; a payment goes by
 * [refresh]'s own result, which is never the stored copy. */
internal class VaziePlusCatalogRepository(
    private val api: VazieApiClient,
    private val store: PlusCatalogStore,
    private val timeoutMillis: Long = TIMEOUT_MILLIS,
) : PlusCatalogRepository {

    private val mutex = Mutex()
    private val _state = MutableStateFlow<PlusCatalogState>(PlusCatalogState.Loading)
    override val state: StateFlow<PlusCatalogState> = _state.asStateFlow()

    override suspend fun refresh(): AccountResult<PlusCatalog> = mutex.withLock {
        val answer = withTimeoutOrNull(timeoutMillis) { api.get(PATH_PLANS, PlansResponseDto.serializer()) }
        val result: AccountResult<Pair<PlusCatalog, PlansResponseDto>> = when (answer) {
            null -> AccountResult.Failure(AccountFailure.Unreachable)
            is ApiResult.Failure -> AccountResult.Failure(accountFailureOf(answer.failure))
            is ApiResult.Success -> PlusCatalogMapper.map(answer.value)
                ?.let { AccountResult.Success(it to answer.value) }
                // Answered, and not with a catalogue a price can be shown from.
                ?: AccountResult.Failure(AccountFailure.Unknown)
        }
        when (result) {
            is AccountResult.Success -> {
                _state.value = PlusCatalogState.Ready(result.value.first, stale = false)
                store.write(result.value.second)
                AccountResult.Success(result.value.first)
            }
            is AccountResult.Failure -> {
                _state.value = when (val current = _state.value) {
                    is PlusCatalogState.Ready -> current.copy(stale = true)
                    PlusCatalogState.Loading, is PlusCatalogState.Unavailable ->
                        store.read()?.let(PlusCatalogMapper::map)?.let { PlusCatalogState.Ready(it, stale = true) }
                            ?: PlusCatalogState.Unavailable(result.reason)
                }
                result
            }
        }
    }

    private companion object {
        const val PATH_PLANS = "/plans?product=vpn"

        /** A plan screen waits this long before it falls back to the stored plans or says they are unavailable. */
        const val TIMEOUT_MILLIS = 10_000L
    }
}

/** The last catalogue the backend gave, as it gave it — read back through [PlusCatalogMapper], so a stored answer is
 * checked exactly as a fresh one. */
internal interface PlusCatalogStore {
    suspend fun read(): PlansResponseDto?
    suspend fun write(response: PlansResponseDto)
}

/** [PlusCatalogStore] in one JSON file. Plain: prices are public. */
internal class FilePlusCatalogStore(
    private val file: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PlusCatalogStore {

    private val mutex = Mutex()

    override suspend fun read(): PlansResponseDto? = mutex.withLock {
        withContext(dispatcher) {
            try {
                if (!file.exists()) return@withContext null
                VazieApiClient.json.decodeFromString(PlansResponseDto.serializer(), file.readText(Charsets.UTF_8))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // A torn or foreign file is no catalogue; the next good answer replaces it.
                null
            }
        }
    }

    override suspend fun write(response: PlansResponseDto) = mutex.withLock {
        withContext(dispatcher) {
            runCatching {
                file.parentFile?.mkdirs()
                val text = VazieApiClient.json.encodeToString(PlansResponseDto.serializer(), response)
                val temporary = File(file.parentFile, file.name + TEMPORARY_SUFFIX)
                temporary.writeText(text, Charsets.UTF_8)
                if (!temporary.renameTo(file)) {
                    file.writeText(text, Charsets.UTF_8)
                    temporary.delete()
                }
            }
            // Not being able to keep a copy costs only the offline fallback; the fresh plans are already shown.
            Unit
        }
    }

    private companion object {
        const val TEMPORARY_SUFFIX = ".tmp"
    }
}
