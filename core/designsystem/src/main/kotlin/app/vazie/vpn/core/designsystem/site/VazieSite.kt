package app.vazie.vpn.core.designsystem.site

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import java.net.URI

/** Pages on the Vazie site the app links to. [url] comes from the build (`vazie.site.url`), https, no trailing slash. */
@Immutable
data class VazieSite(val url: String) {

    /** The host, for matching pages the site sends a browser back to. */
    val host: String = URI(url).host.lowercase()

    val offer: String get() = page("/legal/offer")
    val refunds: String get() = page("/legal/refunds")

    fun page(path: String): String = url + path
}

/** The site this build links to; `:app` provides it from BuildConfig, previews and tests get a placeholder. */
val LocalVazieSite = staticCompositionLocalOf { VazieSite("https://site.example") }
