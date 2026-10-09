package app.vazie.vpn.data.managed

import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.managed.api.ManagedAccess
import app.vazie.vpn.managed.api.ManagedAccessId
import app.vazie.vpn.managed.api.ManagedAccessState
import app.vazie.vpn.managed.api.ManagedConnectionMaterial
import app.vazie.vpn.managed.api.ManagedServerAvailability
import app.vazie.vpn.managed.api.ManagedServerId
import app.vazie.vpn.managed.api.ManagedServerSummary
import app.vazie.vpn.api.Endpoint
import app.vazie.vpn.api.UnknownParameters
import app.vazie.vpn.api.UnsupportedRuntimeFeature
import app.vazie.vpn.api.VpnProfile
import app.vazie.vpn.api.XrayFlow
import app.vazie.vpn.api.XrayOutbound
import app.vazie.vpn.api.XraySecurity
import app.vazie.vpn.api.XrayTransport
import java.time.Instant

/** Turns the backend's structured profile into the outbound model Vazie's Xray engine already runs. */
internal object ManagedProfileMapper {

    fun map(response: AccessResponseDto): ManagedProfileMapping {
        val accessId = runCatching { ManagedAccessId(response.id) }.getOrNull()
            ?: return ManagedProfileMapping.Malformed
        val state = parseState(response.state) ?: return ManagedProfileMapping.Malformed
        val server = mapServer(response.server) ?: return ManagedProfileMapping.Malformed
        val createdAt = parseInstant(response.createdAt) ?: return ManagedProfileMapping.Malformed
        val expiresAt = response.expiresAt?.let {
            parseInstant(it) ?: return ManagedProfileMapping.Malformed
        }
        val outbound = when (val mapped = mapOutbound(response.profile)) {
            is Step.Ok -> mapped.value
            is Step.Unsupported -> return ManagedProfileMapping.Unsupported(mapped.feature)
            Step.Malformed -> return ManagedProfileMapping.Malformed
        }
        return ManagedProfileMapping.Mapped(
            ManagedConnectionMaterial(
                access = ManagedAccess(
                    id = accessId,
                    state = state,
                    server = server,
                    expiresAt = expiresAt,
                ),
                profile = VpnProfile.Xray(
                    id = accessId.profileId,
                    // The server's own display name. Vazie provisioned this and the user did not name
                    // it, so there is nothing else it could honestly be called.
                    name = server.displayName,
                    // No origin: this profile was never stored and never will be. The profile store
                    // refuses to write one, which is what keeps the ownership line intact.
                    origin = null,
                    createdAt = createdAt,
                    outbound = outbound,
                ),
            ),
        )
    }

    /** A Managed Server as the catalogue describes it. */
    fun mapServer(dto: ServerDto): ManagedServerSummary? {
        val id = runCatching { ManagedServerId(dto.id) }.getOrNull() ?: return null
        val speaksVless = dto.protocols.isEmpty() ||
            dto.protocols.any { it.trim().equals(PROTOCOL_VLESS, ignoreCase = true) }
        val availability = when {
            !speaksVless -> ManagedServerAvailability.UNAVAILABLE
            else -> when (dto.status.trim().uppercase()) {
                "AVAILABLE" -> ManagedServerAvailability.AVAILABLE
                "DRAINING" -> ManagedServerAvailability.DRAINING
                else -> ManagedServerAvailability.UNAVAILABLE
            }
        }
        return ManagedServerSummary(
            id = id,
            displayName = dto.displayName,
            regionId = dto.regionId,
            countryCode = dto.countryCode,
            city = dto.city,
            availability = availability,
        )
    }

    /** `REVOKED` is not among the results, and neither is anything unrecognised. */
    private fun parseState(raw: String): ManagedAccessState? =
        when (raw.trim().uppercase()) {
            "PENDING" -> ManagedAccessState.PENDING
            "PROVISIONING" -> ManagedAccessState.PROVISIONING
            "ACTIVE" -> ManagedAccessState.ACTIVE
            "REVOKING" -> ManagedAccessState.REVOKING
            "FAILED" -> ManagedAccessState.FAILED
            else -> null
        }

    private fun parseInstant(raw: String): Instant? = runCatching { Instant.parse(raw) }.getOrNull()

    private fun mapOutbound(dto: ManagedProfileDto): Step<XrayOutbound.Vless> {
        if (!dto.protocol.trim().equals(PROTOCOL_VLESS, ignoreCase = true)) {
            return Step.Unsupported(UnsupportedRuntimeFeature.PROTOCOL)
        }
        if (dto.credential.isBlank()) return Step.Malformed
        val endpoint = runCatching { Endpoint(dto.endpoint.host, dto.endpoint.port) }.getOrNull()
            ?: return Step.Malformed
        val flow = when (val step = mapFlow(dto.flow)) {
            is Step.Ok -> step.value
            is Step.Unsupported -> return step
            Step.Malformed -> return Step.Malformed
        }
        val security = when (val step = mapSecurity(dto.security)) {
            is Step.Ok -> step.value
            is Step.Unsupported -> return step
            Step.Malformed -> return Step.Malformed
        }
        val transport = when (val step = mapTransport(dto.transport)) {
            is Step.Ok -> step.value
            is Step.Unsupported -> return step
            Step.Malformed -> return Step.Malformed
        }
        return Step.Ok(
            XrayOutbound.Vless(
                userId = Secret.of(dto.credential),
                endpoint = endpoint,
                security = security,
                transport = transport,
                flow = flow,
                // A provisioned profile has no query string, so nothing is left unplaced.
                unknownParameters = UnknownParameters.EMPTY,
            ),
        )
    }

    private fun mapFlow(raw: String?): Step<XrayFlow?> = when (raw?.trim()?.lowercase().orEmpty()) {
        // Absent and empty mean the same thing: the operator named no flow. A blank string is what an
        // operator's empty form field produces, and refusing it would be refusing a server that works.
        "" -> Step.Ok(null)
        XrayFlow.XTLS_RPRX_VISION.wireName -> Step.Ok(XrayFlow.XTLS_RPRX_VISION)
        // Never null-and-carry-on. A flow changes what goes on the wire, so running without one the
        // operator asked for is a different connection than the one that was configured.
        else -> Step.Unsupported(UnsupportedRuntimeFeature.FLOW)
    }

    private fun mapSecurity(dto: SecurityDto): Step<XraySecurity> =
        when (dto.kind.trim().uppercase()) {
            "NONE" -> {
                // `XraySecurity.None` carries nothing, so anything else present here has no home.
                val extras = dto.serverName != null || dto.fingerprint != null ||
                    dto.alpn.isNotEmpty() || dto.publicKey != null || dto.shortId != null ||
                    dto.spiderX != null
                if (extras) Step.Unsupported(UnsupportedRuntimeFeature.SECURITY) else Step.Ok(XraySecurity.None)
            }

            "TLS" -> {
                // REALITY's fields on a TLS security would be dropped by the model, so they refuse it.
                val realityFields = dto.publicKey != null || dto.shortId != null || dto.spiderX != null
                if (realityFields) {
                    Step.Unsupported(UnsupportedRuntimeFeature.SECURITY)
                } else {
                    Step.Ok(XraySecurity.Tls(dto.serverName, dto.fingerprint, dto.alpn))
                }
            }

            "REALITY" -> when {
                // The model has no ALPN for REALITY. Today's managed server sends none; the day one
                // does, refusing is what stops Vazie negotiating a handshake nobody configured.
                dto.alpn.isNotEmpty() -> Step.Unsupported(UnsupportedRuntimeFeature.SECURITY)
                // The backend validates this at write time, so its absence here is the backend
                // contradicting itself rather than an operator describing something exotic.
                dto.publicKey.isNullOrBlank() -> Step.Malformed
                else -> Step.Ok(
                    XraySecurity.Reality(
                        serverName = dto.serverName,
                        fingerprint = dto.fingerprint,
                        publicKey = Secret.of(dto.publicKey),
                        // Blank reads as absent, and that changes nothing on the wire: the engine
                        // writes an empty `shortId` for both.
                        shortId = dto.shortId?.takeIf { it.isNotBlank() }?.let { Secret.of(it) },
                        spiderX = dto.spiderX,
                    ),
                )
            }

            else -> Step.Unsupported(UnsupportedRuntimeFeature.SECURITY)
        }

    private fun mapTransport(dto: TransportDto): Step<XrayTransport> =
        when (dto.kind.trim().uppercase()) {
            "TCP" -> {
                val foreign = dto.path != null || dto.host != null ||
                    dto.serviceName != null || dto.multiMode
                if (foreign) {
                    Step.Unsupported(UnsupportedRuntimeFeature.TRANSPORT)
                } else {
                    Step.Ok(XrayTransport.Tcp(dto.headerType))
                }
            }

            "WEBSOCKET" -> {
                val foreign = dto.headerType != null || dto.serviceName != null || dto.multiMode
                when {
                    foreign -> Step.Unsupported(UnsupportedRuntimeFeature.TRANSPORT)
                    dto.path == null -> Step.Malformed
                    else -> Step.Ok(XrayTransport.WebSocket(dto.path, dto.host))
                }
            }

            "GRPC" -> {
                val foreign = dto.headerType != null || dto.path != null || dto.host != null
                when {
                    foreign -> Step.Unsupported(UnsupportedRuntimeFeature.TRANSPORT)
                    dto.serviceName == null -> Step.Malformed
                    else -> Step.Ok(XrayTransport.Grpc(dto.serviceName, dto.multiMode))
                }
            }

            else -> Step.Unsupported(UnsupportedRuntimeFeature.TRANSPORT)
        }

    private const val PROTOCOL_VLESS = "VLESS"

    /** One mapping step: a value, a capability this build lacks, or a response that was not the contract. */
    private sealed interface Step<out T> {
        data class Ok<out T>(val value: T) : Step<T>
        data class Unsupported(val feature: UnsupportedRuntimeFeature) : Step<Nothing>
        data object Malformed : Step<Nothing>
    }
}

/** What the mapper made of one access response. */
internal sealed interface ManagedProfileMapping {

    data class Mapped(val material: ManagedConnectionMaterial) : ManagedProfileMapping

    /** This build cannot run what the operator described. A real outcome, not a defensive branch. */
    data class Unsupported(val feature: UnsupportedRuntimeFeature) : ManagedProfileMapping

    /** The response was not the contract. A backend fault, and nothing a user can act on. */
    data object Malformed : ManagedProfileMapping
}
