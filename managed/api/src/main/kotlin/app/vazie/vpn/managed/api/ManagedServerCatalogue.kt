package app.vazie.vpn.managed.api

import kotlinx.coroutines.flow.Flow

/** The last managed server catalogue Vazie successfully fetched, kept so that something can be shown when it
 * cannot be fetched again. */
interface ManagedServerCatalogueStore {

    /** The cached catalogue, as it changes. */
    fun observe(): Flow<List<ManagedServerSummary>>

    /** The cached catalogue right now, empty when there has never been one. */
    suspend fun catalogue(): List<ManagedServerSummary>

    /** Replace the whole catalogue with a freshly fetched one. */
    suspend fun replace(servers: List<ManagedServerSummary>)
}
