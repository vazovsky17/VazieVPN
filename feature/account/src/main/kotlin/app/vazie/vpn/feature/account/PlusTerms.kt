package app.vazie.vpn.feature.account

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/** An ISO-8601 instant from the backend as a date in the person's time zone and language — "26 сентября 2027
 * г." — or `null` when there is no date or it cannot be read. */
internal fun plusDate(
    iso: String?,
    locale: Locale = Locale.getDefault(),
    zone: ZoneId = ZoneId.systemDefault(),
): String? {
    if (iso.isNullOrBlank()) return null
    val instant = runCatching { Instant.parse(iso) }.getOrNull() ?: return null
    return DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG).withLocale(locale).format(instant.atZone(zone).toLocalDate())
}
