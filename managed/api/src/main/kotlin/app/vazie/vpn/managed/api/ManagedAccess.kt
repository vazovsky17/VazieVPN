package app.vazie.vpn.managed.api

import app.vazie.vpn.api.VpnProfile
import java.time.Instant

/** One account's access to one Managed Server, **without** connection material. */
data class ManagedAccess(
    val id: ManagedAccessId,
    val state: ManagedAccessState,
    val server: ManagedServerSummary,
    val expiresAt: Instant? = null,
)

/** How far along one managed access is. */
enum class ManagedAccessState {
    PENDING,
    PROVISIONING,
    ACTIVE,
    REVOKING,
    FAILED,
    ;

    /** Whether this access can carry a connection right now. */
    val isUsable: Boolean get() = this == ACTIVE
}

/** An access together with the profile it produced. */
data class ManagedConnectionMaterial(
    val access: ManagedAccess,
    val profile: VpnProfile,
)
