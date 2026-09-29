package app.vazie.vpn.feature.settings

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import app.vazie.vpn.core.designsystem.theme.Appearance
import app.vazie.vpn.core.model.SplitTunnel
import app.vazie.vpn.core.model.VazieAppIcon
import app.vazie.vpn.core.model.VazieAppIconPlate
import app.vazie.vpn.core.model.VazieGuideId

@Immutable
data class SettingsUiState(
    val appearance: Appearance,
    val appIcon: VazieAppIcon,
    /** What this device will let Vazie put where. */
    val systemIntegration: SystemIntegrationUi = SystemIntegrationUi(),
    /** Which apps use the tunnel, for the row's subtitle. */
    val splitTunnel: SplitTunnel = SplitTunnel(),
)

/** The build, as four identifiers. */
@Immutable
data class AboutUi(
    val versionName: String,
    val versionCode: Int,
    val flavor: String,
    val buildType: String,
)

/** What a launcher icon actually looks like, in the two layers it is made of. */
@Immutable
data class AppIconArt(
    val plate: Brush,
    @param:DrawableRes val foreground: Int,
    /** The ink a flat mark is drawn in, or `null` for a mark that carries its own colour. */
    val foregroundTint: Color? = null,
)

/** Everywhere in the app that opens something outside it. */
enum class AboutLink(val url: String) {

    /** The person who writes Vazie, as a person. */
    DEVELOPER("https://t.me/vazovsky17"),

    /** The developer's other work, all on one page. */
    FOLIO("https://folio.link/vazovsky"),

    /** The project's public repository. */
    SOURCE("https://github.com/vazovsky17/VazieVPN"),

    /** Releases and what changed in them. */
    CHANNEL("https://t.me/vazieapp"),

    /** The bug-report form: what happened and on which phone. */
    BUG_REPORT("https://forms.gle/uBV3fWieRWSpLych8"),

    /** Payments, refunds and VPN Plus: the support address the offer (vazie.app/legal/offer) names. */
    SUPPORT_EMAIL("mailto:vazovsky.hub@gmail.com"),

    /** Where the server bill can be helped with. */
    BOOSTY("https://boosty.to/vazie"),
}

/** Whether this device can be *asked* to add each surface. */
@Immutable
data class SystemIntegrationUi(
    /** Guides whose platform surface is actually shipped by this application variant. */
    val visibleGuides: Set<VazieGuideId> = VazieGuideId.entries.toSet(),
    val canAddWidget: Boolean = false,
    val canAddQuickSettingsTile: Boolean = false,
    /** Whether there is a notification-settings screen to send somebody to. */
    val canOpenNotificationSettings: Boolean = false,
    /** Whether Android will currently let Vazie show its status notification. */
    val notificationsEnabled: Boolean = false,
)

sealed interface SettingsAction {

    /** Ask the launcher to place the Quick Connect widget. */
    data object AddWidget : SettingsAction

    /** Ask System UI to add the Vazie tile. API 33 and up; see [SystemIntegrationUi]. */
    /** [SystemIntegrationUi]. */
    data object AddQuickSettingsTile : SettingsAction

    /** Open Android's notification settings for Vazie. */
    data object OpenNotificationSettings : SettingsAction
    /** Open the place every guide lives, whether or not it has been acknowledged. */
    data object OpenGuides : SettingsAction

    /** The questions and answers, the same as on vazie.app. */
    data object OpenFaq : SettingsAction
    data object OpenAppearance : SettingsAction
    data object OpenPrivacy : SettingsAction

    data object OpenAbout : SettingsAction

    /** Choose which apps use the tunnel. */
    data object OpenSplitTunnel : SettingsAction

    /** Open the bug-report form. */
    data object ReportBug : SettingsAction
}

/** How Vazie looks: the palette, and the launcher icon on its plate. */
@Immutable
data class AppearanceUiState(
    val appearance: Appearance,
    val appIcon: VazieAppIcon,
    /** The plate as stored; the screen shows it resolved against [appIcon]'s own plates. */
    val appIconPlate: VazieAppIconPlate,
)

sealed interface AppearanceAction {
    data class SelectAppearance(val appearance: Appearance) : AppearanceAction
    data class SelectAppIcon(val icon: VazieAppIcon) : AppearanceAction
    data class SelectAppIconPlate(val plate: VazieAppIconPlate) : AppearanceAction
    data object Back : AppearanceAction
}

@Immutable
data class AboutUiState(val about: AboutUi)

sealed interface AboutAction {
    data class OpenLink(val link: AboutLink) : AboutAction
    data object Back : AboutAction
}
