package app.vazie.vpn.data.managed

import kotlinx.serialization.Serializable

/** The Vazie VPN API's `/v1` wire format, as far as the managed slice needs it. */
@Serializable
internal data class ServersResponseDto(val servers: List<ServerDto> = emptyList())

@Serializable
internal data class ServerDto(
    val id: String,
    val displayName: String,
    val regionId: String,
    val countryCode: String,
    val city: String? = null,
    /** `AVAILABLE`, `DRAINING` or `UNAVAILABLE`. */
    val status: String,
    val protocols: List<String> = emptyList(),
    /** Present for an account with VPN Plus: where the server listens, for the latency check. */
    val endpoint: EndpointDto? = null,
)

/** A preference, not a command: the backend refuses an unavailable server rather than silently choosing
 * another one, which is what lets a client name the country the user pressed. */
@Serializable
internal data class CreateAccessRequestDto(
    val serverId: String? = null,
    val regionId: String? = null,
)

@Serializable
internal data class AccessResponseDto(
    val id: String,
    val state: String,
    val server: ServerDto,
    val createdAt: String,
    val expiresAt: String? = null,
    val profile: ManagedProfileDto,
)

/** The connection material for one managed access. */
@Serializable
internal data class ManagedProfileDto(
    val protocol: String,
    val endpoint: EndpointDto,
    val credential: String,
    val flow: String? = null,
    val security: SecurityDto,
    val transport: TransportDto,
) {
    override fun toString(): String = REDACTED

    companion object {
        const val REDACTED: String = "ManagedProfileDto(***)"
    }
}

@Serializable
internal data class EndpointDto(val host: String, val port: Int)

/** Redacted for the same reason as [ManagedProfileDto]: `publicKey` and `shortId` are Reality secrets,
 * and this type is the only thing that holds them before they become [Secret]. */
@Serializable
internal data class SecurityDto(
    /** `NONE`, `TLS` or `REALITY`. */
    val kind: String,
    val serverName: String? = null,
    val fingerprint: String? = null,
    val alpn: List<String> = emptyList(),
    val publicKey: String? = null,
    val shortId: String? = null,
    val spiderX: String? = null,
) {
    override fun toString(): String = REDACTED

    companion object {
        const val REDACTED: String = "SecurityDto(***)"
    }
}

@Serializable
internal data class TransportDto(
    /** `TCP`, `WEBSOCKET` or `GRPC`. */
    val kind: String,
    val headerType: String? = null,
    val path: String? = null,
    val host: String? = null,
    val serviceName: String? = null,
    val multiMode: Boolean = false,
)
