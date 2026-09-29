package app.vazie.vpn.account

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import app.vazie.vpn.feature.account.AccountSettingsEntry
import app.vazie.vpn.feature.account.accountSignInScreen
import app.vazie.vpn.feature.account.navigateToAccountSignIn
import app.vazie.vpn.feature.account.navigateToPlusPayment
import app.vazie.vpn.feature.account.navigateToPlusPlans
import app.vazie.vpn.feature.account.navigateToPlusManage
import app.vazie.vpn.feature.account.navigateToAccount
import app.vazie.vpn.account.api.PlusPlan

/** Where the navigation graph meets the Vazie Account and VPN Plus. Every build type has it: the account
 * ships in release. */
internal object AccountEntry {

    fun NavGraphBuilder.accountDestinations(navController: NavController) {
        accountSignInScreen(navController)
    }

    fun settingsSection(navController: NavController): (@Composable () -> Unit)? = {
        AccountSettingsEntry(
            onSignIn = navController::navigateToAccountSignIn,
            onOpenPlus = navController::navigateToPlusPlans,
            onManagePlus = navController::navigateToPlusManage,
            onOpenAccount = navController::navigateToAccount,
        )
    }

    /** Pay for [plan], signing in first if needed. */
    fun startPlusPurchase(navController: NavController, plan: PlusPlan) {
        navController.navigateToPlusPayment(plan)
    }
}
