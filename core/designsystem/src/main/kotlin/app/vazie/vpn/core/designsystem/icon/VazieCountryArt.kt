package app.vazie.vpn.core.designsystem.icon

import androidx.annotation.DrawableRes
import app.vazie.vpn.core.designsystem.R

/** The artwork for a country, by its code, and nothing else about the country. */
object VazieCountryArt {

    /** The circular flag for an ISO 3166-1 alpha-2 code, or `null` where there is no artwork yet. */
    @DrawableRes
    fun flag(countryCode: String): Int? = when (countryCode.uppercase()) {
        "NL" -> R.drawable.ic_country_nl
        "DE" -> R.drawable.ic_country_de
        else -> null
    }
}
