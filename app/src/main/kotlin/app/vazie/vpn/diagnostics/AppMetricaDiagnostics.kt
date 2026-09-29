package app.vazie.vpn.diagnostics

import android.content.Context
import app.vazie.vpn.BuildConfig
import app.vazie.vpn.core.analytics.Diagnostics
import app.vazie.vpn.core.analytics.DiagnosticsConsent
import app.vazie.vpn.core.analytics.PaymentEvent
import app.vazie.vpn.data.AppPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import io.appmetrica.analytics.AppMetrica
import io.appmetrica.analytics.AppMetricaConfig
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers

/** The person's answer, kept with the rest of the app's preferences. */
@Singleton
class PreferencesDiagnosticsConsent @Inject constructor(
    private val preferences: AppPreferences,
) : DiagnosticsConsent {

    override val allowed: StateFlow<Boolean?> = preferences.observe()
        .map { it.diagnosticsAllowed }
        .stateIn(
            CoroutineScope(SupervisorJob() + Dispatchers.Default),
            SharingStarted.Eagerly,
            preferences.snapshot().diagnosticsAllowed,
        )

    override suspend fun set(allowed: Boolean) = preferences.setDiagnosticsAllowed(allowed)
}

/** Crash and payment reports through Yandex AppMetrica — and only after the person said yes. */
@Singleton
class AppMetricaDiagnostics @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val consent: DiagnosticsConsent,
) : Diagnostics {

    private val apiKey: String = BuildConfig.APPMETRICA_API_KEY

    @Volatile
    private var activated = false

    /** Follow the answer: activate on the first yes, stop sending on a no. */
    suspend fun follow() {
        if (apiKey.isBlank()) return
        consent.allowed.collect { allowed ->
            when {
                allowed == true && !activated -> activate()
                allowed == true -> AppMetrica.setDataSendingEnabled(true)
                activated -> AppMetrica.setDataSendingEnabled(false)
            }
        }
    }

    override fun payment(event: PaymentEvent) {
        if (!activated || consent.allowed.value != true) return
        AppMetrica.reportEvent(
            EVENT_PAYMENT,
            buildMap {
                put("stage", event.stage.name)
                put("plan", event.plan)
                event.reason?.let { put("reason", it) }
            },
        )
    }

    private fun activate() {
        val config = AppMetricaConfig.newConfigBuilder(apiKey)
            .withCrashReporting(true)
            .withNativeCrashReporting(false)
            .withLocationTracking(false)
            .withAdvIdentifiersTracking(false)
            .withRevenueAutoTrackingEnabled(false)
            .withAppOpenTrackingEnabled(false)
            .withSessionsAutoTrackingEnabled(false)
            .withDataSendingEnabled(true)
            .build()
        AppMetrica.activate(context, config)
        activated = true
    }

    private companion object {
        const val EVENT_PAYMENT = "plus_payment"
    }
}
