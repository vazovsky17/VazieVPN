package app.vazie.vpn.diagnostics

import app.vazie.vpn.StartupTask
import app.vazie.vpn.core.analytics.Diagnostics
import app.vazie.vpn.core.analytics.DiagnosticsConsent
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

/** Diagnostics and the consent behind them. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DiagnosticsModule {

    @Binds
    abstract fun diagnostics(appMetrica: AppMetricaDiagnostics): Diagnostics

    @Binds
    abstract fun consent(preferences: PreferencesDiagnosticsConsent): DiagnosticsConsent

    companion object {
        /** Starts at launch, so crash reporting is on as early as the answer allows. */
        @Provides
        @IntoSet
        fun followConsent(diagnostics: AppMetricaDiagnostics): StartupTask = StartupTask { diagnostics.follow() }
    }
}
