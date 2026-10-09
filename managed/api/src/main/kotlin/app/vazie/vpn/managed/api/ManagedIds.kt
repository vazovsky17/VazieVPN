package app.vazie.vpn.managed.api

import app.vazie.vpn.core.model.ProfileId

/** A Managed Server, as Vazie's own catalogue names it. */
@JvmInline
value class ManagedServerId(val value: String) {
    init {
        require(value.isNotBlank()) { "a managed server id must not be blank" }
    }
}

/** One account's access to one Managed Server. */
@JvmInline
value class ManagedAccessId(val value: String) {

    init {
        require(value.isNotBlank()) { "a managed access id must not be blank" }
    }

    val profileId: ProfileId get() = ProfileId(PROFILE_ID_PREFIX + value)

    companion object {
        const val PROFILE_ID_PREFIX: String = "vazie-managed-"
    }
}
