package app.vazie.vpn.managed.api

/** Everything the client may ask Vazie about managed access. */
interface ManagedAccessRepository {

    /** The Managed Server catalogue for the signed-in account. */
    suspend fun servers(): ManagedResult<List<ManagedServerSummary>>

    /** Makes sure this account has usable access to [serverId], and returns it with its profile. */
    suspend fun ensureAccess(serverId: ManagedServerId): ManagedResult<ManagedConnectionMaterial>

    /** Gives one managed access back to Vazie, removing the client from the server. */
    suspend fun revokeAccess(id: ManagedAccessId): ManagedResult<Unit>
}

/** What one managed operation produced. */
sealed interface ManagedResult<out T> {

    val requestId: String?

    data class Success<out T>(val value: T, override val requestId: String? = null) : ManagedResult<T>

    data class Failure(
        val reason: ManagedAccessFailure,
        override val requestId: String? = null,
    ) : ManagedResult<Nothing>
}
