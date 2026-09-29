package app.vazie.vpn.account.api

/** A VPN Plus plan: one-off access to Vazie's servers for a fixed period, no renewal. [code] is the backend's
 * plan code; the price is the backend's too, and the payment page shows the amount charged. */
enum class PlusPlan(val code: String) {
    MONTHLY("VPN_PLUS_MONTHLY"),
    YEARLY("VPN_PLUS_YEARLY"),
}

/** A payment the backend has opened, and where to send the person to complete it. */
data class PaymentStart(val paymentId: String, val confirmationUrl: String) {
    override fun toString(): String =
        "PaymentStart(paymentId=${paymentId.take(ID_PREFIX)}…, confirmationUrl=<hidden>)"

    private companion object {
        const val ID_PREFIX = 8
    }
}

/** Opening a VPN Plus payment for the signed-in account. */
interface PlusPaymentRepository {

    suspend fun startPayment(plan: PlusPlan, idempotencyKey: String): AccountResult<PaymentStart>

    /** How VPN Plus is bought right now (`GET /plans`); anything unclear falls back to
     * [PlusPurchase.PaymentPage]. */
    suspend fun purchase(): PlusPurchase
}

/** How VPN Plus is bought right now. */
sealed interface PlusPurchase {

    /** On the payment page, opened by [PlusPaymentRepository.startPayment]. */
    data object PaymentPage : PlusPurchase

    /** By writing to the author at [contactUrl]; access is then switched on by hand. */
    data class Contact(val contactUrl: String) : PlusPurchase {
        /** `https://t.me/vazovsky17` → `@vazovsky17`; any other link as it is, without the scheme. */
        val contactLabel: String
            get() = TELEGRAM.matchEntire(contactUrl)?.let { "@${it.groupValues[1]}" }
                ?: contactUrl.removePrefix("https://")

        private companion object {
            val TELEGRAM = Regex("^https://t\\.me/([A-Za-z0-9_]{3,})/?$")
        }
    }
}

/** VPN Plus access the account holds right now. [validUntil] is an ISO-8601 instant, or `null` for access
 * with no end date. */
data class PlusAccess(val validUntil: String?)

/** The subscription from `GET /subscription`: [planCode] `FREE` means no paid plan; [expiresAt] is ISO-8601
 * or null. */
data class PlusSubscription(
    val planCode: String,
    val planName: String,
    val state: String,
    val expiresAt: String?,
) {
    /** Whether this subscription gives VPN Plus now. */
    val active: Boolean get() = planCode != FREE_PLAN && state in ACTIVE_STATES

    private companion object {
        const val FREE_PLAN = "FREE"
        val ACTIVE_STATES = setOf("active", "in_grace", "cancelled")
    }
}

/** Reading the account's current subscription. */
interface PlusSubscriptionRepository {

    suspend fun current(): AccountResult<PlusSubscription>
}
