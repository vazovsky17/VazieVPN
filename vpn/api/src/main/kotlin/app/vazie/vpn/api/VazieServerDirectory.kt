package app.vazie.vpn.api

import app.vazie.vpn.core.model.ProfileId
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** The servers Vazie runs for VPN Plus, as things a person can select and connect to. */
interface VazieServerDirectory {

    /** The servers this account may use, for lists and Home. Empty without VPN Plus. */
    fun observe(): Flow<List<VazieServer>>

    /** Ask the backend for the current list. Failures keep the last good list. */
    suspend fun refresh()

    /** Access to [id], provisioned if needed, as a profile the engine can run. */
    suspend fun resolve(id: ProfileId): VazieServerResolution

    /** Record that a tunnel to [id] came up, for [VazieServer.lastUsedAt]. The counterpart of
     * [VpnProfileRepository.markUsed] for servers that are not stored profiles. */
    suspend fun markUsed(id: ProfileId)

    companion object {
        const val ID_PREFIX: String = "vazie-server:"

        fun owns(id: ProfileId): Boolean = id.value.startsWith(ID_PREFIX)

        fun idOf(serverId: String): ProfileId = ProfileId(ID_PREFIX + serverId)

        fun serverIdOf(id: ProfileId): String? = id.value.takeIf { owns(id) }?.removePrefix(ID_PREFIX)

        /** No Vazie servers at all — a build or a test without an account. */
        val None: VazieServerDirectory = object : VazieServerDirectory {
            override fun observe(): Flow<List<VazieServer>> = flowOf(emptyList())
            override suspend fun refresh() = Unit
            override suspend fun resolve(id: ProfileId): VazieServerResolution =
                VazieServerResolution.Refused(VazieAccessProblem.PLUS_REQUIRED)
            override suspend fun markUsed(id: ProfileId) = Unit
        }
    }
}

/** One Vazie server as a list shows it. */
data class VazieServer(
    val id: ProfileId,
    val name: String,
    val countryCode: String,
    val city: String?,
    /** Can new connections go there now. */
    val available: Boolean,
    /** Where it listens, when the catalogue said so (VPN Plus only). What the latency check dials. */
    val endpoint: ServerEndpoint? = null,
    /** When a tunnel to it last came up on this device. */
    val lastUsedAt: Instant? = null,
) {
    /** The same server as every connection list draws a row. */
    fun toSummary(): ProfileSummary = ProfileSummary(
        id = id,
        name = name,
        engineId = VpnEngineId.XRAY,
        protocolLabel = PROTOCOL_LABEL,
        origin = null,
        countryCode = countryCode,
        lastUsedAt = lastUsedAt,
    )

    companion object {
        const val PROTOCOL_LABEL: String = "VLESS"
    }
}

sealed interface VazieServerResolution {
    data class Ready(val profile: VpnProfile) : VazieServerResolution
    data class Refused(val problem: VazieAccessProblem) : VazieServerResolution
}

/** Why Vazie did not give access. */
enum class VazieAccessProblem {
    /** Nobody is signed in on this device. */
    SIGN_IN_REQUIRED,

    /** The account has no active VPN Plus. */
    PLUS_REQUIRED,

    /** The server takes no new connections right now. */
    SERVER_UNAVAILABLE,

    /** Vazie could not be reached. */
    UNREACHABLE,

    /** Anything else the backend said no to. */
    OTHER,
}
