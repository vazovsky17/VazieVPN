package app.vazie.vpn.account

import android.content.Context
import app.vazie.vpn.StartupTask
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.api.VazieServerDirectory
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.data.managed.ManagedStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import kotlinx.coroutines.flow.distinctUntilChangedBy

/** VPN Plus servers: the catalogue on disk, `POST /access` through the account's own session, and the
 * directory the connection lists and the controller read. */
@Module
@InstallIn(SingletonComponent::class)
object VazieServersModule {

    @Provides
    @Singleton
    fun vazieServers(
        @ApplicationContext context: Context,
        tokens: SessionTokenStore,
        account: AccountRepository,
    ): VazieServerDirectory {
        val catalogue = ManagedStorage.serverCatalogueStore(context.filesDir)
        val access = ManagedStorage.accessRepository(
            context = context,
            baseUrl = AccountModule.VPN_API_BASE_URL,
            tokens = tokens,
            catalogue = catalogue,
        )
        return ManagedStorage.serverDirectory(access, catalogue, account, context.filesDir)
    }

    /** The list is refreshed whenever the account gains or loses VPN Plus, and once at every start. */
    @Provides
    @IntoSet
    fun refreshWithPlus(account: AccountRepository, servers: VazieServerDirectory): StartupTask = StartupTask {
        account.state
            .distinctUntilChangedBy { state ->
                when (state) {
                    is AccountState.SignedIn -> state.account.plus
                    else -> null
                }
            }
            .collect { servers.refresh() }
    }
}
