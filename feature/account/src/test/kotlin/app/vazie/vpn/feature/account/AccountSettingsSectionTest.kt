package app.vazie.vpn.feature.account

import android.content.Context
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import app.vazie.vpn.core.designsystem.tour.LocalVazieTourTargets
import app.vazie.vpn.core.designsystem.tour.VazieTourTargetId
import app.vazie.vpn.core.designsystem.tour.rememberVazieTourTargets
import app.vazie.vpn.core.designsystem.tour.VazieTourTargets
import app.vazie.vpn.feature.account.components.AccountSettingsSection
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/** The account block in Settings offers VPN Plus whether or not somebody is signed in, and is the anchor the
 * first visit to Settings points at. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountSettingsSectionTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val actions = mutableListOf<AccountSectionAction>()
    private var targets: VazieTourTargets? = null

    private fun show(account: AccountUi) {
        compose.setContent {
            VazieTheme {
                val registry = rememberVazieTourTargets().also { targets = it }
                CompositionLocalProvider(LocalVazieTourTargets provides registry) {
                    AccountSettingsSection(
                        state = AccountSectionUiState(account = account),
                        onAction = { actions += it },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `signed out offers VPN Plus`() {
        show(AccountUi.SignedOut)
        compose.onNodeWithText(context.getString(R.string.plus_row_title)).performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.OpenPlus), actions)
    }

    @Test
    fun `signed in offers VPN Plus`() {
        show(AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE"))
        compose.onNodeWithText(context.getString(R.string.plus_row_title)).performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.OpenPlus), actions)
    }

    @Test
    fun `an active VPN Plus opens its management, not the plans`() {
        show(
            AccountUi.SignedIn(
                email = "person@example.com",
                status = "ACTIVE",
                plus = PlusAccess("2027-09-26T00:00:00Z"),
            ),
        )
        compose.onNodeWithText(context.getString(R.string.plus_row_title)).performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.ManagePlus), actions)
    }

    @Test
    fun `the email opens the account screen`() {
        show(AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE"))
        compose.onNodeWithText("person@example.com").performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.OpenAccount), actions)
    }

    @Test
    fun `the block is the anchor of the Settings coach mark`() {
        show(AccountUi.SignedOut)
        assertNotNull(targets!!.bounds(VazieTourTargetId.ACCOUNT_SECTION), "the account block registered no anchor")
    }
}
