package app.vazie.vpn.api

import app.vazie.vpn.core.model.ProfileId
import java.time.Instant

/** A stored profile as a list sees it. */
data class ProfileSummary(
    val id: ProfileId,
    val name: String,
    val engineId: VpnEngineId,
    val protocolLabel: String,
    /** Where this profile came from. */
    val origin: ProfileOrigin? = ProfileOrigin.IMPORTED_LINK,
    /** When this configuration last carried traffic, or `null` if it never has. */
    val lastUsedAt: Instant? = null,
    /** A Vazie server's country, for its mark; null for the user's own configurations. */
    val countryCode: String? = null,
)
