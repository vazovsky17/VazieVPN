package app.vazie.vpn.navigation

import app.vazie.vpn.feature.home.HomeGraphRoute
import app.vazie.vpn.feature.settings.SettingsGraphRoute
import kotlin.reflect.KClass

/** The three top-level destinations — the whole information architecture. Managed and custom connections are
 * separated by section inside Connections, not by a fourth destination. */
enum class TopLevelDestination(val graphRoute: KClass<*>) {
    HOME(HomeGraphRoute::class),
    SETTINGS(SettingsGraphRoute::class),
}
