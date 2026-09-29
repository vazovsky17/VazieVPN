package app.vazie.vpn.core.analytics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Diagnostics sent only with consent: crash reports and the outcome of a VPN Plus payment. Nothing else. */
interface Diagnostics {

    /** How a VPN Plus payment went. Plan and reason only — never an account, email or amount paid. */
    fun payment(event: PaymentEvent)

    companion object {
        /** Sends nothing: tests, previews, and a build without an AppMetrica key. */
        val None: Diagnostics = object : Diagnostics {
            override fun payment(event: PaymentEvent) = Unit
        }
    }
}

data class PaymentEvent(
    val stage: PaymentStage,
    /** The plan's code, e.g. `MONTHLY`. */
    val plan: String,
    /** A closed-set reason for [PaymentStage.FAILED], e.g. `Unreachable`. */
    val reason: String? = null,
)

enum class PaymentStage {
    /** The payment page opened. */
    OPENED,

    /** The page reported success. */
    SUCCEEDED,

    /** The page reported the payment did not go through. */
    NOT_COMPLETED,

    /** Closed before the page finished. */
    CLOSED,

    /** The payment could not be opened. */
    FAILED,
}

/** The person's answer about diagnostics: `null` until asked, then `true` or `false`. Off unless they said
 * yes — nothing is sent before that. */
interface DiagnosticsConsent {
    val allowed: StateFlow<Boolean?>

    suspend fun set(allowed: Boolean)

    companion object {
        /** In memory, starts unanswered. */
        fun inMemory(initial: Boolean? = null): DiagnosticsConsent = object : DiagnosticsConsent {
            override val allowed = MutableStateFlow(initial)
            override suspend fun set(allowed: Boolean) {
                this.allowed.value = allowed
            }
        }
    }
}
