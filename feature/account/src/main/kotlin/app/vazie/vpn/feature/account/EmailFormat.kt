package app.vazie.vpn.feature.account

/** A first, forgiving look at an address before it is sent anywhere. */
internal object EmailFormat {

    fun isPlausible(raw: String): Boolean {
        val email = raw.trim()
        if (email.length > MAX_LENGTH) return false
        val at = email.indexOf('@')
        if (at <= 0 || at != email.lastIndexOf('@')) return false
        return LOCAL.matches(email.substring(0, at)) && DOMAIN.matches(email.substring(at + 1))
    }

    /** Whole addresses to offer while the domain is still being typed: common domains that fit what is there. */
    fun completions(raw: String): List<String> {
        val email = raw.trim()
        if (email.isEmpty() || email.length > MAX_LENGTH) return emptyList()
        val at = email.indexOf('@')
        val local = if (at < 0) email else email.substring(0, at)
        if (!LOCAL.matches(local) || (at >= 0 && at != email.lastIndexOf('@'))) return emptyList()
        val typed = if (at < 0) "" else email.substring(at + 1).lowercase()
        if (typed in KNOWN_DOMAINS) return emptyList()
        return KNOWN_DOMAINS.filter { it.startsWith(typed) }.take(MAX_COMPLETIONS).map { "$local@$it" }
    }

    /** What to tell a person about the address they are typing, while they type it. */
    fun check(raw: String): EmailCheck {
        val email = raw.trim()
        if (email.isEmpty()) return EmailCheck.Empty
        val at = email.indexOf('@')
        if (email.any { it.isWhitespace() } || email.count { it == '@' } > 1 || at == 0) return EmailCheck.Invalid
        val local = if (at < 0) email else email.substring(0, at)
        // While the name is typed a trailing dot is fine.
        if (!(if (at < 0) LOCAL_TYPING else LOCAL).matches(local)) return EmailCheck.Invalid
        if (at < 0) return EmailCheck.Typing
        val domain = email.substring(at + 1).lowercase()
        suggestion(domain)?.let { return EmailCheck.Suggestion(email.substring(0, at + 1) + it) }
        if (isPlausible(email)) return EmailCheck.Ok
        return if (domain.length < MIN_JUDGED_DOMAIN || !domain.contains('.')) EmailCheck.Typing else EmailCheck.Invalid
    }

    private fun suggestion(domain: String): String? {
        if (domain.length < MIN_JUDGED_DOMAIN || domain in KNOWN_DOMAINS) return null
        // "gmail" without the ending.
        KNOWN_DOMAINS.firstOrNull { it.substringBefore('.') == domain }?.let { return it }
        return KNOWN_DOMAINS
            .map { it to distance(domain, it) }
            .filter { (_, d) -> d in 1..MAX_TYPO_DISTANCE }
            .minByOrNull { (_, d) -> d }
            ?.first
    }

    private fun distance(a: String, b: String): Int {
        val row = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var previous = row[0]
            row[0] = i
            for (j in 1..b.length) {
                val saved = row[j]
                row[j] = minOf(row[j] + 1, row[j - 1] + 1, previous + if (a[i - 1] == b[j - 1]) 0 else 1)
                previous = saved
            }
        }
        return row[b.length]
    }

    // The first MAX_COMPLETIONS are what is offered right after "@", so the order matters.
    private val KNOWN_DOMAINS = listOf(
        "gmail.com", "yandex.ru", "mail.ru", "icloud.com", "proton.me", "ya.ru", "bk.ru", "inbox.ru",
        "list.ru", "rambler.ru", "outlook.com", "hotmail.com", "yahoo.com", "protonmail.com",
    )

    /** A practical subset of RFC 5322 and IDN addresses: letters of any script, digits, the usual marks. */
    private val LOCAL_TYPING = Regex("""[\p{L}\p{N}][\p{L}\p{N}._%+-]{0,63}""")
    private val LOCAL =Regex("""[\p{L}\p{N}](?:[\p{L}\p{N}._%+-]{0,62}[\p{L}\p{N}_%+-])?""")
    private val DOMAIN = Regex("""(?:[\p{L}\p{N}](?:[\p{L}\p{N}-]{0,61}[\p{L}\p{N}])?\.)+\p{L}{2,63}""")

    private const val MAX_COMPLETIONS = 5
    private const val MIN_JUDGED_DOMAIN = 4
    private const val MAX_TYPO_DISTANCE = 2
    private const val MAX_LENGTH = 254
}

/** The address as the sign-in screen judges it while it is typed. */
sealed interface EmailCheck {
    data object Empty : EmailCheck
    /** Not finished yet; nothing to say. */
    data object Typing : EmailCheck
    data object Ok : EmailCheck
    data object Invalid : EmailCheck
    /** Probably a typo in a common domain; [email] is the corrected address. */
    data class Suggestion(val email: String) : EmailCheck
}
