package app.vazie.vpn.managed.api

/** A Managed Server as a client is allowed to see it. */
data class ManagedServerSummary(
    val id: ManagedServerId,
    val displayName: String,
    val regionId: String,
    val countryCode: String,
    val city: String? = null,
    val availability: ManagedServerAvailability,
)

/** Whether a Managed Server is taking connections. */
enum class ManagedServerAvailability {
    AVAILABLE,
    DRAINING,
    UNAVAILABLE,
    ;

    /** Whether asking for new access here is worth a round trip. */
    val acceptsNewAccess: Boolean get() = this == AVAILABLE
}
