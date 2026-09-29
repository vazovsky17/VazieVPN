package app.vazie.vpn.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptions
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.model.SplitTunnel
import app.vazie.vpn.core.model.VazieAppIcon
import app.vazie.vpn.core.model.VazieAppIconPlate
import app.vazie.vpn.core.model.VazieGuideId
import kotlinx.collections.immutable.ImmutableList
import kotlinx.serialization.Serializable

const val SETTINGS_DEEP_LINK: String = "vazie-vpn://settings"

@Serializable
data object SettingsGraphRoute

@Serializable
data object SettingsRoute

/** Appearance, which used to be `AdvancedSettingsRoute`. */
@Serializable
data object AppearanceRoute

/** Which apps use the tunnel. */
@Serializable
data object SplitTunnelRoute


/** The permanent list of guides. */
@Serializable
data object GuidesRoute

/** One guide, addressed by its **stable stored id** rather than by an enum ordinal. */
@Serializable
data class GuideDetailRoute(val guideId: String)

/** One product-learning topic. */
@Serializable
data class GettingStartedRoute(val topic: String)

@Serializable
data object PrivacyRoute

/** Questions and answers. */
@Serializable
data object FaqRoute

@Serializable
data object AboutRoute

fun NavController.navigateToSettings(navOptions: NavOptions? = null) =
    navigate(SettingsGraphRoute, navOptions)

/** [appIconArt], [resolvePlate], [systemIntegration], [currentPlan] and [acknowledgedGuides] come from
 * `:app`; [onSubscribe] is a seam, not a purchase: the graph also opens the coming-soon explanation. */
fun NavGraphBuilder.settingsGraph(
    navController: NavController,
    appearance: () -> Appearance,
    appIcon: () -> VazieAppIcon,
    appIconPlate: () -> VazieAppIconPlate,
    acknowledgedGuides: () -> Set<VazieGuideId>,
    appIconArt: (VazieAppIcon, VazieAppIconPlate) -> AppIconArt,
    resolvePlate: (VazieAppIcon, VazieAppIconPlate) -> VazieAppIconPlate,
    systemIntegration: () -> SystemIntegrationUi,
    onAppearanceChange: (Appearance) -> Unit,
    onAppIconChange: (VazieAppIcon) -> Unit,
    onAppIconPlateChange: (VazieAppIconPlate) -> Unit,
    onAcknowledgeGuide: (VazieGuideId) -> Unit,
    onAddWidget: () -> Unit,
    diagnosticsAllowed: () -> Boolean = { false },
    onDiagnosticsChange: (Boolean) -> Unit = {},
    onAddQuickSettingsTile: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onReplayTour: () -> Unit,
    onAddConfiguration: () -> Unit,
    supportedFormats: () -> String,
    about: AboutUi,
    // The Vazie Account block, when this build has one. A slot rather than a dependency: the account lives in
    // `:feature:account`, which R1 keeps out of reach, and only debug builds bind it.
    accountSection: (@Composable () -> Unit)? = null,
    splitTunnel: () -> SplitTunnel = { SplitTunnel() },
    onSplitTunnelChange: (SplitTunnel) -> Unit = {},
) {
    navigation<SettingsGraphRoute>(startDestination = SettingsRoute) {
        composable<SettingsRoute>(
            deepLinks = listOf(navDeepLink<SettingsRoute>(basePath = SETTINGS_DEEP_LINK)),
        ) {
            val context = LocalContext.current
            SettingsRoute(
                appearance = appearance(),
                splitTunnel = splitTunnel(),
                appIcon = appIcon(),
                systemIntegration = systemIntegration(),
                onAddWidget = onAddWidget,
                onAddQuickSettingsTile = onAddQuickSettingsTile,
                onOpenNotificationSettings = onOpenNotificationSettings,
                onOpenGuides = { navController.navigate(GuidesRoute) },
                onOpenFaq = { navController.navigate(FaqRoute) },
                onOpenAppearance = { navController.navigate(AppearanceRoute) },
                onOpenSplitTunnel = { navController.navigate(SplitTunnelRoute) },
                onOpenPrivacy = { navController.navigate(PrivacyRoute) },
                onReportBug = { context.open(AboutLink.BUG_REPORT) },
                onOpenAbout = { navController.navigate(AboutRoute) },
                accountSection = accountSection,
            )
        }
        composable<SplitTunnelRoute> {
            val context = LocalContext.current
            val apps by produceState<ImmutableList<LaunchableApp>?>(initialValue = null) { value = context.launchableApps() }
            var query by remember { mutableStateOf("") }
            val split = splitTunnel()
            SplitTunnelScreen(
                state = SplitTunnelUiState(split = split, apps = apps, query = query),
                onAction = { action ->
                    when (action) {
                        SplitTunnelAction.Back -> navController.popBackStack()
                        is SplitTunnelAction.QueryChanged -> query = action.query
                        else -> onSplitTunnelChange(split.after(action))
                    }
                },
            )
        }
        composable<AppearanceRoute> {
            AppearanceScreen(
                state = AppearanceUiState(
                    appearance = appearance(),
                    appIcon = appIcon(),
                    appIconPlate = appIconPlate(),
                ),
                appIconArt = appIconArt,
                resolvePlate = resolvePlate,
                onAction = { action ->
                    when (action) {
                        is AppearanceAction.SelectAppearance -> onAppearanceChange(action.appearance)
                        is AppearanceAction.SelectAppIcon -> onAppIconChange(action.icon)
                        is AppearanceAction.SelectAppIconPlate -> onAppIconPlateChange(action.plate)
                        AppearanceAction.Back -> navController.popBackStack()
                    }
                },
            )
        }
        composable<GuidesRoute> {
            val integration = systemIntegration()
            GuidesScreen(
                state = GuidesUiState(
                    guides = guideList(
                        acknowledged = acknowledgedGuides(),
                        visible = integration.visibleGuides,
                    ),
                ),
                onAction = { action ->
                    when (action) {
                        is GuidesAction.OpenGuide ->
                            navController.navigate(GuideDetailRoute(action.id.id))
                        is GuidesAction.OpenGettingStarted ->
                            navController.navigate(GettingStartedRoute(action.topic.name))
                        GuidesAction.ReplayTour -> onReplayTour()
                        GuidesAction.Back -> navController.popBackStack()
                    }
                },
            )
        }
        composable<GuideDetailRoute> { entry ->
            // An unknown id is a link from a build that knew a guide this one does not. Going back
            // is the honest answer; throwing would turn somebody else's newer version into a crash.
            val guide = VazieGuideId.fromId(entry.toRoute<GuideDetailRoute>().guideId)
            if (guide == null || guide !in systemIntegration().visibleGuides) {
                LaunchedEffect(entry.id) { navController.popBackStack() }
                return@composable
            }
            GuideDetailScreen(
                state = GuideDetailUiState(
                    id = guide,
                    systemIntegration = systemIntegration(),
                ),
                onAction = { action ->
                    when (action) {
                        is GuideDetailAction.Settings -> when (val inner = action.action) {
                            SettingsAction.AddWidget -> onAddWidget()
                            SettingsAction.AddQuickSettingsTile -> onAddQuickSettingsTile()
                            SettingsAction.OpenNotificationSettings -> onOpenNotificationSettings()
                            // Guides raise only the two platform requests; anything else is a wiring mistake
                            // and does nothing.
                            else -> Unit
                        }

                        is GuideDetailAction.MarkViewed -> onAcknowledgeGuide(action.id)
                        GuideDetailAction.Back -> navController.popBackStack()
                    }
                },
            )
        }
        composable<GettingStartedRoute> { entry ->
            // Unknown topic: a link from a build that knew one this does not. Going back is the
            // honest answer, and it is the same tolerance the guide route already applies.
            val name = entry.toRoute<GettingStartedRoute>().topic
            val topic = GettingStartedTopic.entries.firstOrNull { it.name == name }
            if (topic == null) {
                LaunchedEffect(entry.id) { navController.popBackStack() }
                return@composable
            }
            GettingStartedScreen(
                state = GettingStartedUiState(
                    topic = topic,
                    supportedFormats = supportedFormats(),
                ),
                onAction = { action ->
                    when (action) {
                        // Out to `:app`, which owns the wizard; this module cannot see `:feature:config`
                        // (R1).
                        GettingStartedAction.AddConfiguration -> onAddConfiguration()
                        GettingStartedAction.Back -> navController.popBackStack()
                    }
                },
            )
        }
        composable<PrivacyRoute> {
            PrivacyScreen(
                onBack = { navController.popBackStack() },
                diagnosticsAllowed = diagnosticsAllowed(),
                onDiagnosticsChange = onDiagnosticsChange,
            )
        }
        composable<FaqRoute> {
            FaqScreen(onBack = { navController.popBackStack() })
        }
        composable<AboutRoute> {
            val context = LocalContext.current
            AboutScreen(
                state = AboutUiState(about = about),
                onAction = { action ->
                    when (action) {
                        is AboutAction.OpenLink -> context.open(action.link)
                        AboutAction.Back -> navController.popBackStack()
                    }
                },
            )
        }
    }
}

@Composable
private fun SettingsRoute(
    appearance: Appearance,
    splitTunnel: SplitTunnel,
    appIcon: VazieAppIcon,
    systemIntegration: SystemIntegrationUi,
    onAddWidget: () -> Unit,
    onAddQuickSettingsTile: () -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onOpenGuides: () -> Unit,
    onOpenFaq: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenSplitTunnel: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onReportBug: () -> Unit,
    onOpenAbout: () -> Unit,
    accountSection: (@Composable () -> Unit)?,
) {
    SettingsScreen(
        accountSection = accountSection,
        state = SettingsUiState(
            appearance = appearance,
            appIcon = appIcon,
            systemIntegration = systemIntegration,
            splitTunnel = splitTunnel,
        ),
        onAction = { action ->
            when (action) {
                // Straight through to `:app`, which owns the components both requests name. Neither reports
                // success, so neither changes any state here — see `SystemIntegration`.
                SettingsAction.AddWidget -> onAddWidget()
                SettingsAction.AddQuickSettingsTile -> onAddQuickSettingsTile()
                SettingsAction.OpenNotificationSettings -> onOpenNotificationSettings()
                SettingsAction.OpenGuides -> onOpenGuides()
                SettingsAction.OpenFaq -> onOpenFaq()
                SettingsAction.OpenAppearance -> onOpenAppearance()
                SettingsAction.OpenSplitTunnel -> onOpenSplitTunnel()
                SettingsAction.OpenPrivacy -> onOpenPrivacy()
                SettingsAction.ReportBug -> onReportBug()
                SettingsAction.OpenAbout -> onOpenAbout()
            }
        },
    )
}

/** Opens a link, and does nothing if there is nothing to open it with. */
private fun Context.open(link: AboutLink) {
    runCatching {
        startActivity(Intent(Intent.ACTION_VIEW, link.url.toUri()))
    }.onFailure { failure ->
        if (failure !is ActivityNotFoundException) throw failure
    }
}
