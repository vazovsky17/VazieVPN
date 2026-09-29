package app.vazie.vpn.feature.account

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.designsystem.theme.VazieTheme
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The account screen: signing out lives here, and its VPN Plus row goes to management when the subscription
 * is active and to the plans otherwise. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AccountScreenTest {

    @get:Rule
    val compose = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val actions = mutableListOf<AccountSectionAction>()

    private fun show(account: AccountUi) {
        compose.setContent {
            VazieTheme {
                AccountScreen(
                    state = AccountSectionUiState(account = account),
                    onAction = { actions += it },
                    onBack = {},
                )
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun `sign out is on the account screen`() {
        show(AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE"))
        compose.onNodeWithText("person@example.com").assertExists()
        compose.onNodeWithText(context.getString(R.string.account_sign_out)).performScrollTo().performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.SignOut), actions)
    }

    @Test
    fun `without VPN Plus the row opens the plans`() {
        show(AccountUi.SignedIn(email = "person@example.com", status = "ACTIVE"))
        compose.onNodeWithText(context.getString(R.string.account_plus_row)).performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.OpenPlus), actions)
    }

    @Test
    fun `with VPN Plus the row opens its management`() {
        show(
            AccountUi.SignedIn(
                email = "person@example.com",
                status = "ACTIVE",
                plus = PlusAccess("2027-09-26T00:00:00Z"),
            ),
        )
        compose.onNodeWithText(context.getString(R.string.account_plus_row)).performClick()
        assertEquals(listOf<AccountSectionAction>(AccountSectionAction.ManagePlus), actions)
    }
}
