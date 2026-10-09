package app.vazie.vpn.account.api

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Money is exact, in the person's own format, and the catalogue chooses by period, not by position. */
class PlusCatalogTest {

    private val russian = Locale.forLanguageTag("ru-RU")

    @Test
    fun `roubles are formatted from minor units with the locale's own separators`() {
        // No-break spaces: the amount and the sign never wrap apart.
        assertEquals("299 ₽", PlusPrice(29_900, "RUB").format(russian))
        assertEquals("2 990 ₽", PlusPrice(299_000, "RUB").format(russian))
        assertEquals("349 ₽", PlusPrice(34_900, "RUB").format(russian))
    }

    @Test
    fun `kopecks appear only when there are some`() {
        assertEquals("299,50 ₽", PlusPrice(29_950, "RUB").format(russian))
        assertEquals("0,99 ₽", PlusPrice(99, "RUB").format(russian))
    }

    @Test
    fun `the same amount reads in the language of the phone`() {
        val english = PlusPrice(299_000, "RUB").format(Locale.US)
        assertTrue(english.contains("2,990"), english)
        assertFalse(english.contains("2\u00A0990"), english)
    }

    @Test
    fun `a large amount is exact, not a rounded double`() {
        // 9_007_199_254_740_993 cannot be held by a Double; the digits must still be right.
        val text = PlusPrice(90_071_992_547_409_93, "RUB").format(Locale.US)
        assertTrue(text.contains("90,071,992,547,409.93"), text)
    }

    @Test
    fun `currencies with no or three fraction digits follow their own minor unit`() {
        assertEquals("500", PlusPrice(500, "JPY").format(Locale.US).filter { it.isDigit() })
        assertTrue(PlusPrice(1_500, "KWD").format(Locale.US).contains("1.500"))
    }

    @Test
    fun `an invalid price cannot be built`() {
        assertFailsWith<IllegalArgumentException> { PlusPrice(0, "RUB") }
        assertFailsWith<IllegalArgumentException> { PlusPrice(-1, "RUB") }
        assertFailsWith<IllegalArgumentException> { PlusPrice(100, "rub") }
        assertFailsWith<IllegalArgumentException> { PlusPrice(100, "XXX") }
        assertNull(PlusPrice.currencyOrNull("US"))
    }

    @Test
    fun `a monthly share rounds down to a whole unit and is absent below one`() {
        // 2 990 ₽ / 12 = 249.16… → 249 ₽, never 250.
        assertEquals(PlusPrice(24_900, "RUB"), PlusPrice(299_000, "RUB").perMonth(12))
        assertNull(PlusPrice(1_000, "RUB").perMonth(12))
    }

    @Test
    fun `the default plan is the yearly one by period, in any order`() {
        assertEquals(YEARLY, catalog(MONTHLY, YEARLY).defaultPlan)
        assertEquals(YEARLY, catalog(YEARLY, MONTHLY).defaultPlan)
        assertEquals(MONTHLY, catalog(MONTHLY).defaultPlan)
    }

    @Test
    fun `a plan is found by its code and only by it`() {
        val catalog = catalog(MONTHLY, YEARLY)
        assertEquals(MONTHLY, catalog.plan("VPN_PLUS_MONTHLY"))
        assertNull(catalog.plan("vpn_plus_monthly"))
        assertNull(catalog.plan("MONTHLY"))
        assertNull(catalog.plan(null))
    }

    @Test
    fun `better value compares a month's cost, and only in one currency`() {
        val catalog = catalog(MONTHLY, YEARLY)
        assertTrue(catalog.isBetterValue(YEARLY))
        assertFalse(catalog.isBetterValue(MONTHLY))

        val dearYear = YEARLY.copy(price = PlusPrice(400_000, "RUB"))
        assertFalse(catalog(MONTHLY, dearYear).isBetterValue(dearYear))

        val dollars = YEARLY.copy(price = PlusPrice(100, "USD"))
        assertFalse(catalog(MONTHLY, dollars).isBetterValue(dollars), "prices in two currencies are not comparable")
        assertFalse(catalog(YEARLY).isBetterValue(YEARLY), "nothing to compare with")
    }

    @Test
    fun `an empty catalogue or a repeated code cannot be built`() {
        assertFailsWith<IllegalArgumentException> { PlusCatalog(emptyList(), PlusPurchase.PaymentPage) }
        assertFailsWith<IllegalArgumentException> { catalog(MONTHLY, MONTHLY.copy(title = "again")) }
    }

    private fun catalog(vararg plans: PlusPlan) = PlusCatalog(plans.toList(), PlusPurchase.PaymentPage)

    private companion object {
        val MONTHLY = PlusPlan("VPN_PLUS_MONTHLY", "1 month", PlusBillingPeriod.MONTHLY, 1, PlusPrice(29_900, "RUB"), false)
        val YEARLY = PlusPlan("VPN_PLUS_YEARLY", "1 year", PlusBillingPeriod.YEARLY, 12, PlusPrice(299_000, "RUB"), false)
    }
}
