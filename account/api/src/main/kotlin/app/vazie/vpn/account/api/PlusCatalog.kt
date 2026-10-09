package app.vazie.vpn.account.api

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale
import kotlinx.coroutines.flow.StateFlow

/** How long one VPN Plus payment lasts. Only these two are sold; anything else the backend lists is not VPN Plus
 * as this app knows it, and is left out rather than guessed at. */
enum class PlusBillingPeriod(val months: Int) {
    MONTHLY(1),
    YEARLY(12),
}

/** An amount of money in its currency's minor units — `29900` RUB is 299 ₽. Never a `Double`: the amount is
 * shown exactly as the backend will charge it. */
data class PlusPrice(val amountMinorUnits: Long, val currencyCode: String) {

    init {
        require(amountMinorUnits > 0) { "a price is positive" }
        require(currencyOrNull(currencyCode) != null) { "not an ISO 4217 currency: $currencyCode" }
    }

    private val currency: Currency get() = Currency.getInstance(currencyCode)

    /** `299 ₽` in Russian, with the currency's own digits: fractions only when the amount has them. */
    fun format(locale: Locale): String {
        val digits = currency.defaultFractionDigits.coerceAtLeast(0)
        val amount = BigDecimal.valueOf(amountMinorUnits, digits)
        val whole = amount.stripTrailingZeros().scale() <= 0
        return NumberFormat.getCurrencyInstance(locale).apply {
            currency = this@PlusPrice.currency
            minimumFractionDigits = if (whole) 0 else digits
            maximumFractionDigits = if (whole) 0 else digits
        }.format(amount)
    }

    /** This price spread over [months], rounded down to a whole unit — "about 249 ₽ a month" for 2 990 ₽ a
     * year. Rounded down so the claim is never more generous than the truth; `null` below one unit. */
    fun perMonth(months: Int): PlusPrice? {
        require(months > 0) { "months is positive" }
        val unit = BigDecimal.ONE.movePointRight(currency.defaultFractionDigits.coerceAtLeast(0)).toLong()
        val whole = amountMinorUnits / months / unit * unit
        return if (whole > 0) PlusPrice(whole, currencyCode) else null
    }

    override fun toString(): String = "PlusPrice($amountMinorUnits $currencyCode)"

    companion object {
        /** The currency for [code], or `null` for anything that is not one money is paid in: not an ISO 4217 code, or
         * one of the codes that name no currency (`XXX`, the metals and funds — no minor unit to count in). */
        fun currencyOrNull(code: String): Currency? =
            if (code.length == 3 && code.all { it in 'A'..'Z' }) {
                runCatching { Currency.getInstance(code) }.getOrNull()?.takeIf { it.defaultFractionDigits >= 0 }
            } else {
                null
            }
    }
}

/** One VPN Plus plan as the backend sells it right now. [code] is all that is ever sent back to buy it; the
 * price is shown, never sent. */
data class PlusPlan(
    val code: String,
    val title: String,
    val billingPeriod: PlusBillingPeriod,
    val months: Int,
    val price: PlusPrice,
    val autoRenewAllowed: Boolean,
)

/** The VPN Plus plans on sale and how they are bought — the backend's, and nobody else's. */
data class PlusCatalog(val plans: List<PlusPlan>, val purchase: PlusPurchase) {

    init {
        require(plans.isNotEmpty()) { "a catalogue has plans" }
        require(plans.map { it.code }.toSet().size == plans.size) { "plan codes are unique" }
    }

    fun plan(code: String?): PlusPlan? = plans.firstOrNull { it.code == code }

    /** The plan chosen until the person picks another: the yearly one, found by its period — the backend's
     * order means nothing — or the first there is. */
    val defaultPlan: PlusPlan
        get() = plans.firstOrNull { it.billingPeriod == PlusBillingPeriod.YEARLY } ?: plans.first()

    /** Whether [plan] costs less per month than the monthly plan, in the same currency. Nothing is called
     * better value when there is nothing to compare it with. */
    fun isBetterValue(plan: PlusPlan): Boolean {
        val monthly = plans.firstOrNull { it.billingPeriod == PlusBillingPeriod.MONTHLY } ?: return false
        if (plan == monthly || plan.price.currencyCode != monthly.price.currencyCode) return false
        // Cross-multiplied, so no division rounds the comparison.
        return plan.price.amountMinorUnits * monthly.months < monthly.price.amountMinorUnits * plan.months
    }
}

/** What the plan screens show. */
sealed interface PlusCatalogState {

    /** Nothing stored yet, and the first read is under way. */
    data object Loading : PlusCatalogState

    /** [catalog] to show. [stale] — the backend could not be read just now, and this is the last catalogue
     * that was; a payment re-reads the plans before it starts, so a stale price is never what is charged. */
    data class Ready(val catalog: PlusCatalog, val stale: Boolean) : PlusCatalogState

    /** Nothing stored, and the backend could not be read: the plans are unavailable, and nothing can be bought. */
    data class Unavailable(val failure: AccountFailure) : PlusCatalogState
}

/** The VPN Plus plans, from `GET /plans?product=vpn`, with the last good answer kept on the device. */
interface PlusCatalogRepository {

    val state: StateFlow<PlusCatalogState>

    /** Reads the plans from the backend now. [state] follows: the fresh catalogue, or — on a failure — the last
     * one stored, marked stale. The result is only ever the fresh catalogue or the failure, which is what a
     * payment must go by. */
    suspend fun refresh(): AccountResult<PlusCatalog>
}
