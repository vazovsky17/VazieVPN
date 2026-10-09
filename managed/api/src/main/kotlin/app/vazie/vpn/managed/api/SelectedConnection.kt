package app.vazie.vpn.managed.api

import app.vazie.vpn.core.model.ProfileId

/** What Connect means right now. */
sealed interface SelectedConnection {

    /** A Vazie-operated server. */
    data class Managed(val server: SelectedManagedServer) : SelectedConnection

    /** A configuration the user saved. */
    data class Custom(val id: ProfileId) : SelectedConnection

    /** Nothing chosen yet. */
    data object None : SelectedConnection
}

/** The precedence, in one place. */
fun selectedConnection(
    managed: SelectedManagedServer?,
    custom: ProfileId?,
): SelectedConnection = when {
    managed != null -> SelectedConnection.Managed(managed)
    custom != null -> SelectedConnection.Custom(custom)
    else -> SelectedConnection.None
}
