package app.vazie.vpn.data.managed

import app.vazie.vpn.core.network.ApiFailure
import app.vazie.vpn.core.network.ApiResult
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.core.network.VazieApiClient
import app.vazie.vpn.managed.api.ManagedAccessFailure
import app.vazie.vpn.managed.api.ManagedAccessId
import app.vazie.vpn.managed.api.ManagedAccessRepository
import app.vazie.vpn.managed.api.ManagedConnectionMaterial
import app.vazie.vpn.managed.api.ManagedResult
import app.vazie.vpn.managed.api.ManagedServerCatalogueStore
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary

/** The managed access port, over the Vazie backend's public API. */
internal class VazieManagedAccessRepository(
    private val api: VazieApiClient,
    private val tokens: SessionTokenStore,
    private val catalogue: ManagedServerCatalogueStore,
    private val endpoints: ServerEndpointMemory = ServerEndpointMemory(),
) : ManagedAccessRepository {

    /** The catalogue, and the one place it is cached. */
    override suspend fun servers(): ManagedResult<List<ManagedServerSummary>> {
        if (tokens.token() == null) return ManagedResult.Failure(ManagedAccessFailure.SignInRequired)
        return when (val result = api.get(PATH_SERVERS, ServersResponseDto.serializer())) {
            is ApiResult.Failure -> failure(result.failure)
            is ApiResult.Success -> {
                val servers = result.value.servers.mapNotNull(ManagedProfileMapper::mapServer)
                catalogue.replace(servers)
                endpoints.remember(result.value.servers)
                ManagedResult.Success(value = servers, requestId = result.requestId)
            }
        }
    }

    override suspend fun ensureAccess(
        serverId: ManagedServerId,
    ): ManagedResult<ManagedConnectionMaterial> {
        if (tokens.token() == null) return ManagedResult.Failure(ManagedAccessFailure.SignInRequired)
        val body = VazieApiClient.json.encodeToJsonElement(
            CreateAccessRequestDto.serializer(),
            CreateAccessRequestDto(serverId = serverId.value),
        )
        return when (val result = api.post(PATH_ACCESS, body, AccessResponseDto.serializer())) {
            is ApiResult.Failure -> failure(result.failure)
            is ApiResult.Success -> when (val mapping = ManagedProfileMapper.map(result.value)) {
                is ManagedProfileMapping.Mapped ->
                    ManagedResult.Success(mapping.material, result.requestId)

                is ManagedProfileMapping.Unsupported -> ManagedResult.Failure(
                    ManagedAccessFailure.UnsupportedProfile(mapping.feature),
                    result.requestId,
                )

                // A response that is not the contract. There is nothing truthful to say beyond that
                // it happened, and the request id is what makes it findable in the backend's logs.
                ManagedProfileMapping.Malformed ->
                    ManagedResult.Failure(ManagedAccessFailure.Unknown, result.requestId)
            }
        }
    }

    override suspend fun revokeAccess(id: ManagedAccessId): ManagedResult<Unit> {
        if (tokens.token() == null) return ManagedResult.Failure(ManagedAccessFailure.SignInRequired)
        // The id goes into a URL, so anything but a UUID shape is refused rather than escaped.
        if (!id.value.all { it.isLetterOrDigit() || it == '-' }) {
            return ManagedResult.Failure(ManagedAccessFailure.Unknown)
        }
        return when (val result = api.delete("$PATH_ACCESS/${id.value}")) {
            is ApiResult.Success -> ManagedResult.Success(Unit, result.requestId)
            is ApiResult.Failure -> {
                val mapped = failure(result.failure)
                // Revocation is idempotent: an access that is already gone is success.
                if (mapped.reason == ManagedAccessFailure.AccessNotFound) {
                    ManagedResult.Success(Unit, result.requestId)
                } else {
                    mapped
                }
            }
        }
    }

    private suspend fun failure(failure: ApiFailure): ManagedResult.Failure = ManagedResult.Failure(
        reason = reason(failure),
        requestId = failure.requestId,
    )

    private suspend fun reason(failure: ApiFailure): ManagedAccessFailure = when (failure) {
        is ApiFailure.Unreachable -> ManagedAccessFailure.BackendUnreachable
        // Something answered and it was not the contract. Retrying does not fix it, so it is not
        // reported as a connectivity problem the user could wait out.
        is ApiFailure.Unreadable -> ManagedAccessFailure.Unknown
        is ApiFailure.Http -> httpReason(failure)
    }

    private suspend fun httpReason(failure: ApiFailure.Http): ManagedAccessFailure =
        when (failure.code) {
            CODE_AUTH_REQUIRED -> ManagedAccessFailure.SignInRequired
            CODE_AUTH_INVALID -> {
                tokens.clear()
                ManagedAccessFailure.SessionExpired
            }

            CODE_SUBSCRIPTION_REQUIRED -> ManagedAccessFailure.EntitlementMissing
            CODE_SERVER_UNAVAILABLE, CODE_SERVER_NOT_FOUND -> ManagedAccessFailure.ServerUnavailable
            CODE_VPN_ACCESS_NOT_FOUND -> ManagedAccessFailure.AccessNotFound
            CODE_PROVISIONING_FAILED -> ManagedAccessFailure.ProvisioningFailed
            CODE_RATE_LIMITED -> ManagedAccessFailure.RateLimited(failure.retryAfterSeconds)
            // No recognised code: probably a proxy, not the backend, so only the status is read.
            else -> when (failure.status) {
                STATUS_UNAUTHORIZED -> ManagedAccessFailure.SignInRequired
                STATUS_TOO_MANY_REQUESTS -> ManagedAccessFailure.RateLimited(failure.retryAfterSeconds)
                in STATUS_GATEWAY_RANGE -> ManagedAccessFailure.BackendUnreachable
                else -> ManagedAccessFailure.Unknown
            }
        }

    private companion object {
        const val PATH_SERVERS = "/servers"
        const val PATH_ACCESS = "/access"

        const val CODE_AUTH_REQUIRED = "AUTH_REQUIRED"
        const val CODE_AUTH_INVALID = "AUTH_INVALID"
        const val CODE_SUBSCRIPTION_REQUIRED = "SUBSCRIPTION_REQUIRED"
        const val CODE_SERVER_UNAVAILABLE = "SERVER_UNAVAILABLE"
        const val CODE_SERVER_NOT_FOUND = "SERVER_NOT_FOUND"
        const val CODE_VPN_ACCESS_NOT_FOUND = "VPN_ACCESS_NOT_FOUND"
        const val CODE_PROVISIONING_FAILED = "PROVISIONING_FAILED"
        const val CODE_RATE_LIMITED = "RATE_LIMITED"

        const val STATUS_UNAUTHORIZED = 401
        const val STATUS_TOO_MANY_REQUESTS = 429
        val STATUS_GATEWAY_RANGE = 502..504
    }
}
