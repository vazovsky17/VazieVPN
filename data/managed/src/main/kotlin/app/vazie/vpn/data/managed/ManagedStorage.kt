package app.vazie.vpn.data.managed

import android.content.Context
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.core.network.VazieApiClient
import app.vazie.vpn.managed.api.ManagedAccessRepository
import app.vazie.vpn.managed.api.ManagedServerCatalogueStore
import app.vazie.vpn.managed.api.SelectedManagedServerStore
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.api.VazieServerDirectory
import java.io.File

/** How `:app` builds what this module provides. The one public door, matching `ProfileStorage`. */
object ManagedStorage {

    // Shared by the repository that fetches the catalogue and the directory that shows it.
    private val endpoints = ServerEndpointMemory()

    /** The managed access port, and the API client it speaks through. */
    fun accessRepository(
        context: Context,
        baseUrl: ApiBaseUrl,
        tokens: SessionTokenStore,
        catalogue: ManagedServerCatalogueStore,
    ): ManagedAccessRepository = VazieManagedAccessRepository(
        api = VazieApiClient.create(context = context, baseUrl = baseUrl, tokens = tokens),
        tokens = tokens,
        catalogue = catalogue,
        endpoints = endpoints,
    )

    /** The catalogue cache, beside the other managed files and readable without a session. */
    /** The VPN Plus servers the connection lists show, over [access] and [catalogue]. */
    fun serverDirectory(
        access: ManagedAccessRepository,
        catalogue: ManagedServerCatalogueStore,
        account: AccountRepository,
        filesDir: File,
    ): VazieServerDirectory = ManagedServerDirectory(
        access = access,
        catalogue = catalogue,
        account = account,
        endpoints = endpoints,
        usage = FileServerUsage(File(directory(filesDir), USAGE_FILE_NAME)),
    )

    fun serverCatalogueStore(filesDir: File): ManagedServerCatalogueStore =
        FileManagedServerCatalogue(File(directory(filesDir), CATALOGUE_FILE_NAME))

    /** Which Vazie server Connect would use. Plain: a catalogue identifier is not a secret. */
    fun selectedServerStore(filesDir: File): SelectedManagedServerStore =
        FileSelectedManagedServer(File(directory(filesDir), SELECTED_SERVER_FILE_NAME))

    private fun directory(filesDir: File): File = File(filesDir, DIRECTORY)

    private const val DIRECTORY = "vazie-managed"
    private const val SELECTED_SERVER_FILE_NAME = "selected-server"
    private const val CATALOGUE_FILE_NAME = "server-catalogue"
    private const val USAGE_FILE_NAME = "server-usage"
}
