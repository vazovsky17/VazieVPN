package app.vazie.vpn.managed.api

import app.vazie.vpn.api.ConnectionFailure

/** Pressing Connect on a Vazie-operated server, from the point of view of whoever pressed it. */
interface ManagedConnect {

    /** Ensures access to [serverId] and starts a connection to it. */
    suspend fun connect(serverId: ManagedServerId): ManagedConnectOutcome
}

/** What came of pressing Connect. */
sealed interface ManagedConnectOutcome {

    /** The tunnel is starting. Its progress is on the connection state, not here. */
    data object Started : ManagedConnectOutcome

    /** Android has not been asked for VPN consent yet. */
    data object PermissionRequired : ManagedConnectOutcome

    /** Vazie declined to provide access. */
    data class Refused(val failure: ManagedAccessFailure) : ManagedConnectOutcome

    /** Access was provided and this device could not connect with it. */
    data class NotConnected(val failure: ConnectionFailure) : ManagedConnectOutcome
}
