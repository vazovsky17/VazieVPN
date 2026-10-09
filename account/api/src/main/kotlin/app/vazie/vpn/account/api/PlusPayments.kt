package app.vazie.vpn.account.api

/** How VPN Plus is bought right now, read with the plans (`GET /plans`); anything unclear is
 * [PlusPurchase.PaymentPage]. The app only passes the plan on to the site, which acts on this. */
sealed interface PlusPurchase {

    /** On the payment page. */
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
 * or null. [access] is the VPN Plus the account holds right now, from the same answer's entitlements — bought or
 * granted in the admin panel; a grant comes with no paid plan at all. */
data class PlusSubscription(
    val planCode: String,
    val planName: String,
    val state: String,
    val expiresAt: String?,
    val access: PlusAccess? = null,
) {
    /** Whether the account has VPN Plus now: the entitlement says so, whatever the plan — as Settings reads it. */
    val active: Boolean get() = access != null || (hasPlan && state in ACTIVE_STATES)

    /** When VPN Plus ends: the access's end when the entitlement is there (`null` — no end date), else the plan's. */
    val activeUntil: String? get() = if (access != null) access.validUntil else expiresAt

    /** A paid plan is behind it; a grant from the admin panel has none to name. */
    val hasPlan: Boolean get() = planCode != FREE_PLAN

    private companion object {
        const val FREE_PLAN = "FREE"
        val ACTIVE_STATES = setOf("active", "in_grace", "cancelled")
    }
}

/** Reading the account's current subscription. */
interface PlusSubscriptionRepository {

    suspend fun current(): AccountResult<PlusSubscription>
}
