package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.core.network.ApiFailure

/** What a failed request to Vazie's backend means for the account and for VPN Plus — one reading of the
 * backend's error codes, shared so sign-in and the subscription never disagree about them. */
internal fun accountFailureOf(failure: ApiFailure): AccountFailure = when (failure) {
    is ApiFailure.Unreachable -> AccountFailure.Unreachable
    is ApiFailure.Unreadable -> AccountFailure.Unknown
    is ApiFailure.Http -> when (failure.code) {
        CODE_EMAIL_INVALID -> AccountFailure.InvalidEmail
        CODE_EMAIL_CODE_INVALID -> AccountFailure.InvalidCode
        CODE_RATE_LIMITED -> AccountFailure.RateLimited(failure.retryAfterSeconds)
        CODE_EMAIL_UNAVAILABLE -> AccountFailure.EmailUnavailable
        CODE_NOT_ENABLED -> AccountFailure.NotEnabled
        CODE_AUTH_INVALID, CODE_AUTH_REQUIRED -> AccountFailure.SessionExpired
        // No recognised code: probably not the backend answering. Read the status for the little it
        // can honestly say, as `VazieManagedAccessRepository` does.
        else -> when (failure.status) {
            STATUS_UNAUTHORIZED -> AccountFailure.SessionExpired
            STATUS_TOO_MANY_REQUESTS -> AccountFailure.RateLimited(failure.retryAfterSeconds)
            in STATUS_GATEWAY_RANGE -> AccountFailure.Unreachable
            else -> AccountFailure.Unknown
        }
    }
}

private const val CODE_EMAIL_INVALID = "EMAIL_INVALID"
private const val CODE_EMAIL_CODE_INVALID = "EMAIL_CODE_INVALID"
private const val CODE_RATE_LIMITED = "RATE_LIMITED"
private const val CODE_EMAIL_UNAVAILABLE = "EMAIL_UNAVAILABLE"
private const val CODE_NOT_ENABLED = "NOT_ENABLED"
private const val CODE_AUTH_INVALID = "AUTH_INVALID"
private const val CODE_AUTH_REQUIRED = "AUTH_REQUIRED"

private const val STATUS_UNAUTHORIZED = 401
private const val STATUS_TOO_MANY_REQUESTS = 429
private val STATUS_GATEWAY_RANGE = 502..504
