package app.vazie.vpn.feature.account

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalUriHandler
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.vazie.vpn.core.designsystem.site.LocalVazieSite
import app.vazie.vpn.feature.account.components.AccountSettingsSection
import kotlinx.serialization.Serializable

/** Email sign-in, one destination for both steps; "back" on the code step returns to the address.
 * [thenPlan] is the code of a VPN Plus plan to pay for right after signing in. */
@Serializable
data class AccountSignInRoute(val thenPlan: String? = null)

/** The VPN Plus plans. */
@Serializable
data object PlusPlansRoute

/** The account: address, status, VPN Plus, sign-out. */
@Serializable
data object AccountRoute

/** Deleting the account, apart from the account screen. */
@Serializable
data object DeleteAccountRoute

/** Managing the active VPN Plus subscription. */
@Serializable
data object PlusManageRoute

/** Paying for [plan], a plan code from the backend's catalogue (`VPN_PLUS_YEARLY`), on the site. */
@Serializable
data class PlusPaymentRoute(val plan: String)

fun NavController.navigateToAccountSignIn() = navigate(AccountSignInRoute()) { launchSingleTop = true }

fun NavController.navigateToPlusPlans() = navigate(PlusPlansRoute) { launchSingleTop = true }

fun NavController.navigateToAccount() = navigate(AccountRoute) { launchSingleTop = true }

fun NavController.navigateToPlusManage() = navigate(PlusManageRoute) { launchSingleTop = true }

/** Pay for the plan [planCode]: straight to the site when signed in, through sign-in first when not — the
 * payment screen decides, so every entry point behaves the same. */
fun NavController.navigateToPlusPayment(planCode: String) = navigate(PlusPaymentRoute(planCode)) { launchSingleTop = true }

fun NavGraphBuilder.accountSignInScreen(navController: NavController) {
    composable<AccountSignInRoute> { entry ->
        val viewModel: SignInViewModel = hiltViewModel(viewModelStoreOwner = entry)
        val thenPlan = entry.toRoute<AccountSignInRoute>().thenPlan
        SignInRoute(
            viewModel = viewModel,
            onFinished = { signedIn ->
                if (signedIn && thenPlan != null) {
                    navController.navigate(PlusPaymentRoute(thenPlan)) {
                        popUpTo<AccountSignInRoute> { inclusive = true }
                    }
                } else {
                    navController.popBackStack()
                }
            },
        )
    }
    composable<AccountRoute> {
        val viewModel: AccountSectionViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        // Signed out from here, or elsewhere: nothing left to show.
        LaunchedEffect(state.account) {
            if (state.account == AccountUi.SignedOut) navController.popBackStack()
        }
        AccountScreen(
            state = state,
            onAction = { action ->
                when (action) {
                    AccountSectionAction.ManagePlus -> navController.navigateToPlusManage()
                    AccountSectionAction.OpenPlus -> navController.navigateToPlusPlans()
                    AccountSectionAction.OpenDeleteAccount ->
                        navController.navigate(DeleteAccountRoute) { launchSingleTop = true }
                    else -> viewModel.onAction(action)
                }
            },
            onBack = { navController.popBackStack() },
        )
    }
    composable<DeleteAccountRoute> {
        val viewModel: AccountSectionViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        // Deleted, or signed out elsewhere: close this screen and the account screen under it.
        LaunchedEffect(state.account) {
            if (state.account == AccountUi.SignedOut) navController.popBackStack<AccountRoute>(inclusive = true)
        }
        DeleteAccountScreen(state = state, onAction = viewModel::onAction, onBack = { navController.popBackStack() })
    }
    composable<PlusManageRoute> {
        val viewModel: PlusManageViewModel = hiltViewModel()
        val plans: PlusCatalogViewModel = hiltViewModel()
        val state by viewModel.state.collectAsStateWithLifecycle()
        val catalog by plans.state.collectAsStateWithLifecycle()
        PlusManageScreen(
            state = state,
            catalog = catalog,
            onExtend = { code -> navController.navigateToPlusPayment(code) },
            onRetry = viewModel::retry,
            onRetryCatalog = plans::retry,
            onBack = { navController.popBackStack() },
        )
    }
    composable<PlusPlansRoute> {
        val plans: PlusCatalogViewModel = hiltViewModel()
        val catalog by plans.state.collectAsStateWithLifecycle()
        PlusPlansScreen(
            catalog = catalog,
            onConnect = { code -> navController.navigateToPlusPayment(code) },
            onRetry = plans::retry,
            onBack = { navController.popBackStack() },
        )
    }
    composable<PlusPaymentRoute> { entry ->
        val viewModel: PlusPaymentViewModel = hiltViewModel(viewModelStoreOwner = entry)
        val plan = entry.toRoute<PlusPaymentRoute>().plan
        PlusPaymentRoute(
            viewModel = viewModel,
            onSignInFirst = {
                navController.navigate(AccountSignInRoute(thenPlan = plan)) {
                    popUpTo<PlusPaymentRoute> { inclusive = true }
                }
            },
            onFinished = { navController.popBackStack() },
        )
    }
}

@Composable
private fun PlusPaymentRoute(
    viewModel: PlusPaymentViewModel,
    onSignInFirst: () -> Unit,
    onFinished: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    val site = LocalVazieSite.current
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                PlusPaymentEffect.SignInFirst -> onSignInFirst()
                is PlusPaymentEffect.OpenSite -> {
                    val opened = runCatching { uriHandler.openUri(site.vpnPlus(effect.planCode)) }.isSuccess
                    viewModel.onAction(if (opened) PlusPaymentAction.SiteOpened else PlusPaymentAction.SiteNotOpened)
                }
                PlusPaymentEffect.Finished -> onFinished()
            }
        }
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.onAction(PlusPaymentAction.Resumed)
        onPauseOrDispose {}
    }
    PlusPaymentScreen(state = state, onAction = viewModel::onAction, onBack = onFinished)
}

@Composable
private fun SignInRoute(viewModel: SignInViewModel, onFinished: (signedIn: Boolean) -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SignInEffect.SignedIn -> onFinished(true)
                SignInEffect.Close -> onFinished(false)
            }
        }
    }
    BackHandler(enabled = state.step == SignInStep.CODE) {
        viewModel.onAction(SignInAction.ChangeEmail)
    }
    when (state.step) {
        SignInStep.EMAIL -> EmailScreen(state = state, onAction = viewModel::onAction)
        SignInStep.CODE -> CodeScreen(state = state, onAction = viewModel::onAction)
    }
}

/** The account block for Settings, with its own view model. `:feature:settings` never sees this module (R1);
 * `:app` hands this composable to it as a slot. */
@Composable
fun AccountSettingsEntry(
    onSignIn: () -> Unit,
    onOpenPlus: () -> Unit,
    onManagePlus: () -> Unit,
    onOpenAccount: () -> Unit,
) {
    val viewModel: AccountSectionViewModel = hiltViewModel()
    val state by viewModel.state.collectAsStateWithLifecycle()
    AccountSettingsSection(
        state = state,
        onAction = { action ->
            when (action) {
                AccountSectionAction.SignIn -> onSignIn()
                AccountSectionAction.OpenPlus -> onOpenPlus()
                AccountSectionAction.ManagePlus -> onManagePlus()
                AccountSectionAction.OpenAccount -> onOpenAccount()
                else -> viewModel.onAction(action)
            }
        },
    )
}
