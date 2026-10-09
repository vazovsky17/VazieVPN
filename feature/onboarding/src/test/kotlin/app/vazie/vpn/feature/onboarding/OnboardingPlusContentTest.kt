package app.vazie.vpn.feature.onboarding

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.PlusBillingPeriod
import app.vazie.vpn.account.api.PlusCatalog
import app.vazie.vpn.account.api.PlusCatalogState
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPrice
import app.vazie.vpn.account.api.PlusPurchase
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.core.designsystem.site.VazieSite
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.feature.onboarding.components.OnboardingPlusContent
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The last onboarding page sells what the backend sells: its plans and prices, the year by default, and — with no
 * catalogue — a wait or an error with a retry, never a price of the app's own. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "ru")
class OnboardingPlusContentTest {

    @get:Rule
    val compose = createComposeRule()

    private val picked = mutableListOf<String>()
    private var retries = 0

    private fun show(plans: PlusCatalogState, selected: PlusPlan?) {
        compose.setContent {
            CompositionLocalProvider(LocalVazieSite provides VazieSite("https://vazie.app")) {
                VazieTheme {
                    OnboardingPlusContent(
                        plans = plans,
                        selected = selected,
                        onSelect = { picked += it },
                        onRetry = { retries++ },
                        consent = false,
                        onConsentChange = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `the backend's plans are shown at the backend's prices, the year selected`() {
        // Sample amounts that exist nowhere in the app's resources.
        val catalog = catalog(monthly = 41_700, yearly = 350_000)
        show(PlusCatalogState.Ready(catalog, stale = false), catalog.defaultPlan)

        compose.onNodeWithText("417 ₽").assertIsDisplayed()
        compose.onNodeWithText("3 500 ₽").assertIsDisplayed()
        compose.onNodeWithText("Около 291 ₽ в месяц").assertIsDisplayed()
        compose.onNode(hasText("1 год") and isSelectable()).assertIsSelected()
    }

    @Test
    fun `picking a plan reports its code`() {
        val catalog = catalog()
        show(PlusCatalogState.Ready(catalog, stale = false), catalog.defaultPlan)

        compose.onNode(hasText("1 месяц")).performClick()

        assertEquals(listOf("VPN_PLUS_MONTHLY"), picked)
    }

    @Test
    fun `while the plans load, the page waits and shows no price`() {
        show(PlusCatalogState.Loading, selected = null)

        compose.onNodeWithText("Загружаем тарифы…").assertIsDisplayed()
        assertTrue(compose.onAllNodes(hasText("₽", substring = true)).fetchSemanticsNodes().isEmpty())
    }

    @Test
    fun `with no plans and no connection the page says so and can retry`() {
        show(PlusCatalogState.Unavailable(AccountFailure.Unreachable), selected = null)

        compose.onNodeWithText("Без связи с Vazie тарифы не загрузить.", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Повторить").performScrollTo().performClick()
        assertEquals(1, retries)
        assertTrue(compose.onAllNodes(hasText("₽", substring = true)).fetchSemanticsNodes().isEmpty(), "a price with no catalogue")
    }

    @Test
    fun `stored plans are marked as such`() {
        val catalog = catalog()
        show(PlusCatalogState.Ready(catalog, stale = true), catalog.defaultPlan)

        compose.onNodeWithText("Цены — из последней загрузки тарифов; актуальные будут на сайте перед оплатой.").assertIsDisplayed()
    }

    private fun catalog(monthly: Long = 29_900, yearly: Long = 299_000) = PlusCatalog(
        listOf(
            PlusPlan("VPN_PLUS_MONTHLY", "1 month", PlusBillingPeriod.MONTHLY, 1, PlusPrice(monthly, "RUB"), false),
            PlusPlan("VPN_PLUS_YEARLY", "1 year", PlusBillingPeriod.YEARLY, 12, PlusPrice(yearly, "RUB"), false),
        ),
        PlusPurchase.PaymentPage,
    )
}
