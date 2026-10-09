package app.vazie.vpn.data.managed

import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.api.VazieAccessProblem
import app.vazie.vpn.api.VazieServer
import app.vazie.vpn.api.VazieServerDirectory
import app.vazie.vpn.api.VazieServerResolution
import app.vazie.vpn.api.VpnProfile
import app.vazie.vpn.core.model.ProfileId
import app.vazie.vpn.managed.api.ManagedAccessFailure
import app.vazie.vpn.managed.api.ManagedAccessRepository
import app.vazie.vpn.managed.api.ManagedResult
import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerCatalogueStore
import app.vazie.vpn.managed.api.ManagedServerId
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** VPN Plus servers for the connection lists, and access to one of them at connect time. */
internal class ManagedServerDirectory(
    private val access: ManagedAccessRepository,
    private val catalogue: ManagedServerCatalogueStore,
    private val account: AccountRepository,
    private val endpoints: ServerEndpointMemory = ServerEndpointMemory(),
    private val usage: FileServerUsage? = null,
    private val clock: Clock = Clock.systemUTC(),
) : VazieServerDirectory {

    private val resolving = Mutex()

    override fun observe(): Flow<List<VazieServer>> =
        combine(
            account.state,
            catalogue.observe(),
            endpoints.current,
            usage?.observe() ?: flowOf(emptyMap()),
        ) { state, servers, addresses, used ->
            if (!state.hasPlus()) {
                emptyList()
            } else {
                servers
                    .filter { it.availability != ManagedServerAvailability.UNAVAILABLE }
                    .map { server ->
                        VazieServer(
                            id = VazieServerDirectory.idOf(server.id.value),
                            name = server.displayName,
                            countryCode = server.countryCode,
                            city = server.city,
                            available = server.availability == ManagedServerAvailability.AVAILABLE,
                            endpoint = addresses[server.id.value],
                            lastUsedAt = used[server.id.value],
                        )
                    }
            }
        }.distinctUntilChanged()

    override suspend fun refresh() {
        if (account.state.value.hasPlus()) access.servers()
    }

    override suspend fun resolve(id: ProfileId): VazieServerResolution = resolving.withLock {
        val serverId = VazieServerDirectory.serverIdOf(id)
            ?.let { runCatching { ManagedServerId(it) }.getOrNull() }
            ?: return VazieServerResolution.Refused(VazieAccessProblem.OTHER)
        when (val result = access.ensureAccess(serverId)) {
            is ManagedResult.Success -> {
                val profile = result.value.profile
                val name = catalogue.catalogue().firstOrNull { it.id == serverId }?.displayName ?: profile.name
                when (profile) {
                    is VpnProfile.Xray -> VazieServerResolution.Ready(profile.copy(id = id, name = name))
                }
            }
            is ManagedResult.Failure -> VazieServerResolution.Refused(result.reason.toProblem())
        }
    }

    override suspend fun markUsed(id: ProfileId) {
        val serverId = VazieServerDirectory.serverIdOf(id) ?: return
        usage?.markUsed(serverId, clock.instant())
    }

    private fun AccountState.hasPlus(): Boolean = when (this) {
        is AccountState.SignedIn -> account.plus != null
        is AccountState.Offline -> account?.plus != null
        else -> false
    }

    private fun ManagedAccessFailure.toProblem(): VazieAccessProblem = when (this) {
        ManagedAccessFailure.SignInRequired, ManagedAccessFailure.SessionExpired -> VazieAccessProblem.SIGN_IN_REQUIRED
        ManagedAccessFailure.EntitlementMissing -> VazieAccessProblem.PLUS_REQUIRED
        ManagedAccessFailure.ServerUnavailable -> VazieAccessProblem.SERVER_UNAVAILABLE
        ManagedAccessFailure.BackendUnreachable -> VazieAccessProblem.UNREACHABLE
        ManagedAccessFailure.AccessNotFound,
        ManagedAccessFailure.ProvisioningFailed,
        is ManagedAccessFailure.RateLimited,
        is ManagedAccessFailure.UnsupportedProfile,
        ManagedAccessFailure.Unknown,
        -> VazieAccessProblem.OTHER
    }
}
