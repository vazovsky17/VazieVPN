package app.vazie.vpn.data.account

import android.content.Context
import android.os.Build
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.PlusCatalogRepository
import app.vazie.vpn.account.api.PlusSubscriptionRepository
import app.vazie.vpn.core.crypto.KeystoreCipher
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenProvider
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.core.network.VazieApiClient
import java.io.File
import java.util.Locale

/** How `:app` builds what this module provides. The one public door, matching `ManagedStorage`. */
object AccountStorage {

    /** The session token store. One per process: the account signs in through it, and managed access will
     * spend the same session. */
    fun sessionTokenStore(filesDir: File): SessionTokenStore = FileSessionTokenStore(
        file = File(File(filesDir, DIRECTORY), SESSION_FILE_NAME),
        // Its own key, not the profile store's: clearing a session must not touch a profile.
        cipher = KeystoreCipher(alias = SESSION_KEY_ALIAS),
    )

    fun repository(
        context: Context,
        baseUrl: ApiBaseUrl,
        tokens: SessionTokenStore,
    ): AccountRepository = VazieAccountRepository(
        api = VazieApiClient.create(context = context, baseUrl = baseUrl, tokens = tokens),
        tokens = tokens,
        deviceName = deviceName(Build.MANUFACTURER, Build.MODEL),
    )

    /** The VPN Plus plans and prices, the backend's only. Public: read without a session, and the last good answer
     * is kept beside the account files for when the backend cannot be reached. */
    fun plusCatalog(
        context: Context,
        baseUrl: ApiBaseUrl,
        filesDir: File,
    ): PlusCatalogRepository = VaziePlusCatalogRepository(
        api = VazieApiClient.create(context = context, baseUrl = baseUrl, tokens = SessionTokenProvider { null }),
        store = FilePlusCatalogStore(File(File(filesDir, DIRECTORY), PLUS_CATALOG_FILE_NAME)),
    )

    /** The account's current subscription. */
    fun plusSubscription(
        context: Context,
        baseUrl: ApiBaseUrl,
        tokens: SessionTokenStore,
    ): PlusSubscriptionRepository = VaziePlusSubscriptionRepository(
        api = VazieApiClient.create(context = context, baseUrl = baseUrl, tokens = tokens),
    )

    /** This phone's name in the device list: manufacturer and model only, capped at 64 characters. */
    internal fun deviceName(manufacturer: String?, model: String?): String? {
        val maker = manufacturer?.trim().orEmpty()
        val name = model?.trim().orEmpty()
        val combined = when {
            name.isEmpty() -> maker
            maker.isEmpty() || name.startsWith(maker, ignoreCase = true) -> name
            else -> "$maker $name"
        }
        return combined
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
            .take(MAX_DEVICE_NAME)
            .trim()
            .ifEmpty { null }
    }

    private const val DIRECTORY = "vazie-account"
    private const val SESSION_FILE_NAME = "session"
    private const val PLUS_CATALOG_FILE_NAME = "plus-catalog.json"
    private const val SESSION_KEY_ALIAS = "vazie.account.session.v1"
    private const val MAX_DEVICE_NAME = 64
}
