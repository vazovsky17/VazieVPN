package app.vazie.vpn.account.api

import app.vazie.vpn.core.model.Secret
import kotlinx.coroutines.flow.StateFlow

/** The Vazie Account on this device: signing in with an emailed code, knowing who is signed in, and signing
 * out. */
interface AccountRepository {

    val state: StateFlow<AccountState>

    /** Asks the backend who the stored session belongs to. Called once at cold start and whenever the user
     * retries after an offline answer. */
    suspend fun refresh(): AccountState

    suspend fun requestEmailCode(email: String): AccountResult<EmailCodeRequested>

    suspend fun verifyEmailCode(email: String, code: Secret<String>): AccountResult<Account>

    /** Ends the session on the backend, then forgets it here. */
    suspend fun signOut(): AccountResult<Unit>

    /** Forgets the session on this device without asking the backend. */
    suspend fun signOutLocally()

    /** Deletes the account on the backend — its address, sessions and VPN Plus access — then forgets the
     * session here. Irreversible; the caller asks first. */
    suspend fun deleteAccount(): AccountResult<Unit>
}

/** Who is signed in, as far as this device knows. */
sealed interface AccountState {

    /** Not yet asked: the first answer at cold start is still on its way. */
    data object Unknown : AccountState

    data object SignedOut : AccountState

    data class SignedIn(val account: Account) : AccountState

    /** A session is stored but the backend could not confirm it. [account] is what this process last learnt,
     * or `null` after a cold start — nothing about the account is kept on disk. */
    data class Offline(val account: Account?) : AccountState
}

/** The signed-in account, as `GET /me` describes it. */
data class Account(
    val id: String,
    val email: String?,
    val status: String?,
    val entitlements: List<String>,
    val plus: PlusAccess? = null,
) {
    override fun toString(): String =
        "Account(id=${id.take(ID_PREFIX)}…, status=$status, entitlements=$entitlements, plus=$plus)"

    private companion object {
        const val ID_PREFIX = 8
    }
}

/** A code went out; "send again" may be offered after [resendAfterSeconds]. */
data class EmailCodeRequested(val resendAfterSeconds: Long)

sealed interface AccountResult<out T> {
    data class Success<out T>(val value: T) : AccountResult<T>
    data class Failure(val reason: AccountFailure) : AccountResult<Nothing>
}

/** Why an account operation did not happen, in the words a screen needs. */
sealed interface AccountFailure {

    /** The backend does not accept the address. */
    data object InvalidEmail : AccountFailure

    /** The code is wrong, has expired, or its attempts are used up. The backend answers all three with one
     * error on purpose, so this cannot tell them apart either. */
    data object InvalidCode : AccountFailure

    /** Too soon or too often. [retryAfterSeconds] when the backend said. */
    data class RateLimited(val retryAfterSeconds: Long?) : AccountFailure

    /** The backend cannot send mail right now. */
    data object EmailUnavailable : AccountFailure

    /** Email sign-in is switched off on this backend. */
    data object NotEnabled : AccountFailure

    /** The session is no longer valid. */
    data object SessionExpired : AccountFailure

    /** Nothing answered. */
    data object Unreachable : AccountFailure

    /** Something answered and it was not the contract. */
    data object Unknown : AccountFailure
}
