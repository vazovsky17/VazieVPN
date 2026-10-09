package app.vazie.vpn.managed.api

import kotlinx.coroutines.flow.Flow

/** The Vazie server the user chose, with enough to draw it before anything is connected. */
data class SelectedManagedServer(
    val id: ManagedServerId,
    val displayName: String,
    val countryCode: String,
)

/** Which Vazie-operated server Connect would use, if any. */
interface SelectedManagedServerStore {

    fun observeSelected(): Flow<SelectedManagedServer?>

    suspend fun selected(): SelectedManagedServer?

    suspend fun select(server: SelectedManagedServer)

    suspend fun clear()
}
