package app.vazie.vpn.core.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** The ids are a boundary format, and a boundary format is a promise to everything on the other side. */
class VaziePlanIdTest {

    @Test
    fun `the three canonical plans carry their stable ids`() {
        assertEquals(
            mapOf(
                VaziePlanId.FREE to "free",
                VaziePlanId.CONNECT to "connect",
                VaziePlanId.CORE to "core",
            ),
            VaziePlanId.entries.associateWith { it.id },
        )
    }

    @Test
    fun `a plan round-trips through its id`() {
        VaziePlanId.entries.forEach { plan ->
            assertEquals(plan, VaziePlanId.fromId(plan.id))
        }
        assertNull(VaziePlanId.fromId("premium"))
        assertNull(VaziePlanId.fromId(""))
    }

    /** Only `Vazie Free` is unpaid, and that is a property of the plan rather than of the build. */
    @Test
    fun `only the free plan is unpaid`() {
        assertFalse(VaziePlanId.FREE.isPaid)
        assertTrue(VaziePlanId.CONNECT.isPaid)
        assertTrue(VaziePlanId.CORE.isPaid)
    }

    @Test
    fun `the four billing periods carry their stable ids and lengths`() {
        assertEquals(
            mapOf(
                VazieBillingPeriod.P1M to ("p1m" to 1),
                VazieBillingPeriod.P3M to ("p3m" to 3),
                VazieBillingPeriod.P6M to ("p6m" to 6),
                VazieBillingPeriod.P12M to ("p12m" to 12),
            ),
            VazieBillingPeriod.entries.associateWith { it.id to it.months },
        )
    }

    @Test
    fun `a period round-trips through its id`() {
        VazieBillingPeriod.entries.forEach { period ->
            assertEquals(period, VazieBillingPeriod.fromId(period.id))
        }
        assertNull(VazieBillingPeriod.fromId("p24m"))
    }

    /** The shortest period is the baseline, so it is the one period that can state no saving. */
    @Test
    fun `only the shortest period cannot save`() {
        assertFalse(VazieBillingPeriod.P1M.canSave)
        assertTrue(VazieBillingPeriod.P3M.canSave)
        assertTrue(VazieBillingPeriod.P6M.canSave)
        assertTrue(VazieBillingPeriod.P12M.canSave)
    }

    /** Ids are unique — the property that makes `fromId` a function rather than a guess. */
    @Test
    fun `no two ids collide`() {
        assertEquals(
            VaziePlanId.entries.size,
            VaziePlanId.entries.map { it.id }.toSet().size,
        )
        assertEquals(
            VazieBillingPeriod.entries.size,
            VazieBillingPeriod.entries.map { it.id }.toSet().size,
        )
    }
}
