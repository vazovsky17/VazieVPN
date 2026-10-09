package app.vazie.vpn.core.designsystem.site

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import java.net.URLEncoder

/** Pages on the Vazie site the app links to. [url] comes from the build (`vazie.site.url`), no trailing slash: https
 * in a release, and a local address while a build is tried on a device. */
@Immutable
data class VazieSite(val url: String) {

    val offer: String get() = page("/legal/offer")
    val refunds: String get() = page("/legal/refunds")

    /** Where VPN Plus is bought: the site opens on the plan [planCode] and decides how it is paid. */
    fun vpnPlus(planCode: String): String = page("/vpn/plus?plan=${URLEncoder.encode(planCode, "UTF-8")}")

    fun page(path: String): String = url + path
}

/** The site this build links to; `:app` provides it from BuildConfig, previews and tests get a placeholder. */
val LocalVazieSite = staticCompositionLocalOf { VazieSite("https://site.example") }
