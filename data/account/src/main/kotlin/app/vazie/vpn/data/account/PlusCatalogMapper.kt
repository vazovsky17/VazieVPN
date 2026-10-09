package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusCatalog
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPrice
import app.vazie.vpn.account.api.PlusPurchase
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull

/** `GET /plans?product=vpn` read into a [PlusCatalog], or `null` when the answer is not one a price can be shown
 * from. What is refused, and why:
 * - a plan that is not VPN, whose period is not one VPN Plus is sold for (or whose months disagree with it),
 *   whose currency is not ISO 4217, or whose amount is not a positive whole number of minor units — left out,
 *   the rest of the catalogue stands;
 * - two plans under one code — the whole answer: which of the two prices is real cannot be told;
 * - nothing left to sell — the whole answer: an empty catalogue is not a catalogue. */
internal object PlusCatalogMapper {

    fun map(response: PlansResponseDto): PlusCatalog? {
        val plans = response.plans.mapNotNull { element -> (element as? JsonObject)?.let(::plan) }
        if (plans.isEmpty()) return null
        if (plans.map { it.code }.toSet().size != plans.size) return null
        return PlusCatalog(plans = plans, purchase = purchase(response.purchase))
    }

    /** How VPN Plus is bought: the contact link only when it is a clear `CONTACT` with an `https` link. */
    fun purchase(dto: PurchaseDto?): PlusPurchase {
        val url = dto?.contactUrl
        return if (dto?.mode == MODE_CONTACT && url != null && url.startsWith(HTTPS)) {
            PlusPurchase.Contact(url)
        } else {
            PlusPurchase.PaymentPage
        }
    }

    /** One plan, read field by field with the type the contract gives it: an amount sent as a string, a fractional
     * amount or a `null` is not an amount. */
    private fun plan(json: JsonObject): PlusPlan? {
        val code = json.text("code")?.takeIf { CODE.matches(it) } ?: return null
        val title = json.text("title")?.trim()?.takeIf { it.isNotEmpty() && it.length <= MAX_TITLE } ?: return null
        if (json.has("product") && json.text("product") != PRODUCT_VPN) return null
        val period = PlusBillingPeriod.entries.firstOrNull { it.name == json.text("billingPeriod") } ?: return null
        if (json.has("months") && json.number("months") != period.months.toLong()) return null
        val amount = json.number("amountMinorUnits")?.takeIf { it in 1..MAX_AMOUNT_MINOR_UNITS } ?: return null
        val currency = json.text("currency")?.takeIf { PlusPrice.currencyOrNull(it) != null } ?: return null
        val autoRenew = (json["autoRenewAllowed"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: false
        return PlusPlan(
            code = code,
            title = title,
            billingPeriod = period,
            months = period.months,
            price = PlusPrice(amount, currency),
            autoRenewAllowed = autoRenew,
        )
    }

    private fun JsonObject.has(key: String) = key in this && this[key] !is JsonNull

    private fun JsonObject.text(key: String): String? = (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content

    /** A whole JSON number; not a quoted one, not a fraction. */
    private fun JsonObject.number(key: String): Long? = (this[key] as? JsonPrimitive)?.takeIf { !it.isString }?.longOrNull

    /** As the backend's `PlanCode`: upper case, digits and underscores. */
    private val CODE = Regex("^[A-Z0-9_]{1,64}$")
    private const val MAX_TITLE = 100
    private const val PRODUCT_VPN = "VPN"
    private const val MODE_CONTACT = "CONTACT"
    private const val HTTPS = "https://"

    /** A hundred million in minor units — a million roubles. Anything above is a broken answer, not a price. */
    private const val MAX_AMOUNT_MINOR_UNITS = 100_000_000L
}
