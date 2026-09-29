package app.vazie.vpn.account

import android.content.Context
import app.vazie.vpn.BuildConfig
import app.vazie.vpn.StartupTask
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.PlusPaymentRepository
import app.vazie.vpn.account.api.PlusSubscriptionRepository
import app.vazie.vpn.core.network.ApiBaseUrl
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.data.account.AccountStorage
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton

/** The Vazie Account and VPN Plus payments, bound in every build type. */
@Module
@InstallIn(SingletonComponent::class)
object AccountModule {

    /** The shared Vazie API: sign-in, the account, payments. One session token works on both APIs. */
    val SHARED_API_BASE_URL = ApiBaseUrl(BuildConfig.SHARED_API_URL)

    /** The Vazie VPN API: servers, access, the VPN subscription. */
    val VPN_API_BASE_URL = ApiBaseUrl(BuildConfig.VPN_API_URL)

    @Provides
    @Singleton
    fun sessionTokens(@ApplicationContext context: Context): SessionTokenStore =
        AccountStorage.sessionTokenStore(filesDir = context.filesDir)

    @Provides
    @Singleton
    fun account(
        @ApplicationContext context: Context,
        tokens: SessionTokenStore,
    ): AccountRepository = AccountStorage.repository(
        context = context,
        baseUrl = SHARED_API_BASE_URL,
        tokens = tokens,
    )

    @Provides
    @Singleton
    fun plusPayments(
        @ApplicationContext context: Context,
        tokens: SessionTokenStore,
    ): PlusPaymentRepository = AccountStorage.plusPayments(
        context = context,
        baseUrl = SHARED_API_BASE_URL,
        tokens = tokens,
    )

    @Provides
    @Singleton
    fun plusSubscription(
        @ApplicationContext context: Context,
        tokens: SessionTokenStore,
    ): PlusSubscriptionRepository = AccountStorage.plusSubscription(
        context = context,
        baseUrl = VPN_API_BASE_URL,
        tokens = tokens,
    )

    /** Cold start: who does the stored session belong to, if there is one. */
    @Provides
    @IntoSet
    fun refreshAtStartup(account: AccountRepository): StartupTask = StartupTask { account.refresh() }
}
