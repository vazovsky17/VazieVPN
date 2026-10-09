package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusPrice
import app.vazie.vpn.account.api.PlusPurchase
import app.vazie.vpn.core.network.VazieApiClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** `GET /plans?product=vpn` into a catalogue: what is kept, what is left out, and what refuses the whole answer. */
class PlusCatalogMapperTest {

    @Test
    fun `the backend's plans become the catalogue, amounts in minor units`() {
        val catalog = PlusCatalogMapper.map(response(MONTHLY, YEARLY))!!

        assertEquals(listOf("VPN_PLUS_MONTHLY", "VPN_PLUS_YEARLY"), catalog.plans.map { it.code })
        assertEquals(listOf(PlusBillingPeriod.MONTHLY, PlusBillingPeriod.YEARLY), catalog.plans.map { it.billingPeriod })
        assertEquals(listOf(1, 12), catalog.plans.map { it.months })
        assertEquals(listOf(PlusPrice(29_900, "RUB"), PlusPrice(299_000, "RUB")), catalog.plans.map { it.price })
        assertEquals("Vazie VPN Plus — доступ на 1 год", catalog.plans[1].title)
        assertEquals(PlusPurchase.PaymentPage, catalog.purchase)
    }

    @Test
    fun `the yearly plan is the default by its period, wherever the backend lists it`() {
        val yearlyFirst = PlusCatalogMapper.map(response(YEARLY, MONTHLY))!!
        assertEquals("VPN_PLUS_YEARLY", yearlyFirst.defaultPlan.code)

        val onlyMonthly = PlusCatalogMapper.map(response(MONTHLY))!!
        assertEquals("VPN_PLUS_MONTHLY", onlyMonthly.defaultPlan.code)
    }

    @Test
    fun `an unknown period, a plan of another product or an unreadable one is left out and the rest stands`() {
        val catalog = PlusCatalogMapper.map(
            response(
                MONTHLY,
                plan(code = "VPN_PLUS_LIFETIME", period = "LIFETIME", months = null),
                plan(code = "VPN_PLUS_WEEKLY", period = "WEEKLY", months = 0),
                plan(code = "KEEP_PLUS_MONTHLY", product = "KEEP"),
                """{"code":42,"amountMinorUnits":"lots"}""",
            ),
        )!!

        assertEquals(listOf("VPN_PLUS_MONTHLY"), catalog.plans.map { it.code })
    }

    @Test
    fun `an unknown currency is left out`() {
        for (currency in listOf("XXX", "RU", "rub", "RUBLES", "", "€€€")) {
            val catalog = PlusCatalogMapper.map(response(MONTHLY, plan(code = "VPN_PLUS_YEARLY", period = "YEARLY", months = 12, currency = currency)))!!
            assertEquals(listOf("VPN_PLUS_MONTHLY"), catalog.plans.map { it.code }, "kept a plan priced in '$currency'")
        }
        assertNull(PlusCatalogMapper.map(response(plan(currency = "ZZZ"))), "a catalogue of nothing valid is no catalogue")
    }

    @Test
    fun `an invalid amount is left out`() {
        for (amount in listOf("0", "-100", "999999999999", "1.5", "\"299\"", "null")) {
            val catalog = PlusCatalogMapper.map(
                response(MONTHLY, plan(code = "VPN_PLUS_YEARLY", period = "YEARLY", months = 12, amount = amount)),
            )!!
            assertEquals(listOf("VPN_PLUS_MONTHLY"), catalog.plans.map { it.code }, "kept a plan priced at $amount")
        }
    }

    @Test
    fun `months that disagree with the period leave the plan out`() {
        val catalog = PlusCatalogMapper.map(response(MONTHLY, plan(code = "VPN_PLUS_YEARLY", period = "YEARLY", months = 6)))!!
        assertEquals(listOf("VPN_PLUS_MONTHLY"), catalog.plans.map { it.code })
    }

    @Test
    fun `an empty catalogue is refused`() {
        assertNull(PlusCatalogMapper.map(response()))
        assertNull(PlusCatalogMapper.map(VazieApiClient.json.decodeFromString(PlansResponseDto.serializer(), """{}""")))
    }

    @Test
    fun `two plans under one code refuse the whole answer, since which price is real cannot be told`() {
        val duplicated = response(MONTHLY, plan(code = "VPN_PLUS_MONTHLY", amount = "1000"), YEARLY)
        assertNull(PlusCatalogMapper.map(duplicated))
    }

    @Test
    fun `how it is bought is read from the same answer, and only a clear contact answer is a contact`() {
        val contact = PlusCatalogMapper.map(response(MONTHLY, purchase = """{"mode":"CONTACT","contactUrl":"https://t.me/vazovsky17"}"""))!!
        val purchase = assertIs<PlusPurchase.Contact>(contact.purchase)
        assertEquals("@vazovsky17", purchase.contactLabel)

        for (purchaseJson in listOf(
            """{"mode":"PROVIDER"}""",
            """{"mode":"CONTACT"}""",
            """{"mode":"CONTACT","contactUrl":"http://t.me/x"}""",
            """{"mode":"BARTER","contactUrl":"https://t.me/x"}""",
            null,
        )) {
            assertEquals(PlusPurchase.PaymentPage, PlusCatalogMapper.map(response(MONTHLY, purchase = purchaseJson))!!.purchase, "$purchaseJson")
        }
    }

    @Test
    fun `a better value is the plan that costs less a month than the monthly one`() {
        val catalog = PlusCatalogMapper.map(response(MONTHLY, YEARLY))!!
        val (monthly, yearly) = catalog.plans

        assertEquals(true, catalog.isBetterValue(yearly))
        assertEquals(false, catalog.isBetterValue(monthly))

        // A year priced above twelve months is not called better, whatever its position.
        val dear = PlusCatalogMapper.map(response(MONTHLY, plan(code = "VPN_PLUS_YEARLY", period = "YEARLY", months = 12, amount = "400000")))!!
        assertEquals(false, dear.isBetterValue(dear.plans[1]))
        assertEquals(PlusPrice(29_900, "RUB").perMonth(1), PlusPrice(29_900, "RUB"))
    }

    private fun response(vararg plans: String, purchase: String? = null): PlansResponseDto =
        VazieApiClient.json.decodeFromString(
            PlansResponseDto.serializer(),
            """{"plans":[${plans.joinToString(",")}]${purchase?.let { ""","purchase":$it""" } ?: ""}}""",
        )

    private companion object {
        fun plan(
            code: String = "VPN_PLUS_MONTHLY",
            product: String = "VPN",
            period: String = "MONTHLY",
            months: Int? = 1,
            amount: String = "29900",
            currency: String = "RUB",
        ) = """{"code":"$code","title":"Vazie VPN Plus — доступ","product":"$product","productName":"Vazie VPN",""" +
            """"billingPeriod":"$period",${months?.let { "\"months\":$it," } ?: ""}"amountMinorUnits":$amount,""" +
            """"currency":"$currency","autoRenewAllowed":false}"""

        val MONTHLY = plan()
        val YEARLY = """{"code":"VPN_PLUS_YEARLY","title":"Vazie VPN Plus — доступ на 1 год","product":"VPN","productName":"Vazie VPN",""" +
            """"billingPeriod":"YEARLY","months":12,"amountMinorUnits":299000,"currency":"RUB","autoRenewAllowed":false}"""
    }
}
