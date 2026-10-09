package app.vazie.vpn.feature.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
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
import androidx.compose.runtime.CompositionLocalProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The plan screen shows the backend's plans and prices in the person's language, chooses the year by its period,
 * and buys nothing without a catalogue. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "ru")
class PlusPlansScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private var catalog by mutableStateOf<PlusCatalogState>(PlusCatalogState.Loading)
    private val connected = mutableListOf<String>()
    private var retries = 0

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalVazieSite provides VazieSite("https://vazie.app")) {
                VazieTheme {
                    PlusPlansScreen(
                        catalog = catalog,
                        onConnect = { connected += it },
                        onRetry = { retries++ },
                        onBack = {},
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun consent() {
        compose.onNode(hasText("Я принимаю", substring = true)).performScrollTo().performClick()
    }

    @Test
    fun `while the plans load there is a wait and nothing to buy`() {
        show()

        compose.onNodeWithText("Загружаем тарифы…").assertIsDisplayed()
        compose.onNodeWithText("Подключить").performScrollTo().assertIsNotEnabled()
    }

    @Test
    fun `the backend's prices are shown in roubles, not the app's own`() {
        // Sample amounts that exist nowhere in the app's resources: they can only come from the catalogue.
        catalog = PlusCatalogState.Ready(catalog(monthly = 41_700, yearly = 350_000), stale = false)
        show()

        compose.onNodeWithText("417 ₽").assertIsDisplayed()
        compose.onNodeWithText("3 500 ₽").assertIsDisplayed()
        // The monthly share is computed from the yearly price, rounded down: 3 500 / 12 = 291,66… → 291.
        compose.onNodeWithText("Около 291 ₽ в месяц").assertIsDisplayed()
    }

    @Test
    fun `the year is chosen by its period, however the backend orders the plans`() {
        catalog = PlusCatalogState.Ready(catalog(order = listOf(YEARLY_CODE, MONTHLY_CODE)), stale = false)
        show()

        compose.onNode(hasText("1 год")).assertIsDisplayed()
        compose.onNode(hasText("1 год") and isSelectable()).assertIsSelected()
        compose.onNode(hasText("1 месяц") and isSelectable()).assertIsNotSelected()
    }

    @Test
    fun `connecting sends the chosen plan's code and never an amount`() {
        catalog = PlusCatalogState.Ready(catalog(), stale = false)
        show()
        consent()

        compose.onNodeWithText("Подключить").performScrollTo().assertIsEnabled().performClick()

        assertEquals(listOf(YEARLY_CODE), connected)
    }

    @Test
    fun `another plan can be picked, and the code follows`() {
        catalog = PlusCatalogState.Ready(catalog(), stale = false)
        show()
        consent()

        compose.onNode(hasText("1 месяц")).performScrollTo().performClick()
        compose.onNodeWithText("Подключить").performScrollTo().performClick()

        assertEquals(listOf(MONTHLY_CODE), connected)
    }

    @Test
    fun `a stored catalogue is shown, with a word that it may be old and a way to refresh`() {
        catalog = PlusCatalogState.Ready(catalog(), stale = true)
        show()

        compose.onNodeWithText("299 ₽").assertIsDisplayed()
        compose.onNodeWithText("Цены — из последней загрузки тарифов. Актуальные будут на сайте перед оплатой.").assertIsDisplayed()
        compose.onNodeWithText("Обновить цены").performScrollTo().performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `with no catalogue and no connection there is an honest error, a retry, and no price`() {
        catalog = PlusCatalogState.Unavailable(AccountFailure.Unreachable)
        show()

        compose.onNodeWithText("Без связи с Vazie тарифы не загрузить. Проверьте интернет и попробуйте снова.").assertIsDisplayed()
        compose.onNodeWithText("Повторить").performScrollTo().performClick()
        assertEquals(1, retries)
        assertTrue(compose.onAllNodesWithTextCompat("₽").isEmpty(), "a price was shown with no catalogue")
        consent()
        compose.onNodeWithText("Подключить").performScrollTo().assertIsNotEnabled()
        assertTrue(connected.isEmpty())
    }

    @Test
    fun `a server-side problem is worded differently from having no network`() {
        catalog = PlusCatalogState.Unavailable(AccountFailure.Unknown)
        show()

        compose.onNodeWithText("Тарифы временно недоступны. Попробуйте чуть позже.").assertIsDisplayed()
    }

    @Test
    fun `when the retry finds the plans the screen becomes usable`() {
        catalog = PlusCatalogState.Unavailable(AccountFailure.Unreachable)
        show()
        catalog = PlusCatalogState.Ready(catalog(), stale = false)
        compose.waitForIdle()
        consent()

        compose.onNodeWithText("Подключить").performScrollTo().assertIsEnabled()
    }

    private fun androidx.compose.ui.test.junit4.ComposeContentTestRule.onAllNodesWithTextCompat(text: String) =
        onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes()

    private fun catalog(
        monthly: Long = 29_900,
        yearly: Long = 299_000,
        order: List<String> = listOf(MONTHLY_CODE, YEARLY_CODE),
    ): PlusCatalog {
        val plans = mapOf(
            MONTHLY_CODE to PlusPlan(MONTHLY_CODE, "VPN Plus — 1 month", PlusBillingPeriod.MONTHLY, 1, PlusPrice(monthly, "RUB"), false),
            YEARLY_CODE to PlusPlan(YEARLY_CODE, "VPN Plus — 1 year", PlusBillingPeriod.YEARLY, 12, PlusPrice(yearly, "RUB"), false),
        )
        return PlusCatalog(order.map { plans.getValue(it) }, PlusPurchase.PaymentPage)
    }

    private companion object {
        const val MONTHLY_CODE = "VPN_PLUS_MONTHLY"
        const val YEARLY_CODE = "VPN_PLUS_YEARLY"
    }
}
