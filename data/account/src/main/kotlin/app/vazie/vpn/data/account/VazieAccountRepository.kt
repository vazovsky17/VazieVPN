package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.Account
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.EmailCodeRequested
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.model.Secret
import app.vazie.vpn.core.network.ApiFailure
import app.vazie.vpn.core.network.ApiResult
import app.vazie.vpn.core.network.SessionTokenStore
import app.vazie.vpn.core.network.VazieApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** [AccountRepository] against the Vazie backend's email sign-in, `GET /me` and `DELETE /session`. */
internal class VazieAccountRepository(
    private val api: VazieApiClient,
    private val tokens: SessionTokenStore,
    private val deviceName: String?,
) : AccountRepository {

    private val _state = MutableStateFlow<AccountState>(AccountState.Unknown)
    override val state: StateFlow<AccountState> = _state.asStateFlow()

    private val mutex = Mutex()

    override suspend fun refresh(): AccountState = mutex.withLock { refreshLocked() }

    override suspend fun requestEmailCode(email: String): AccountResult<EmailCodeRequested> {
        val body = VazieApiClient.json.encodeToJsonElement(
            EmailStartRequestDto.serializer(),
            EmailStartRequestDto(email = email.trim()),
        )
        return when (val result = api.post(PATH_EMAIL_START, body, EmailStartResponseDto.serializer())) {
            is ApiResult.Failure -> AccountResult.Failure(reason(result.failure))
            is ApiResult.Success ->
                AccountResult.Success(EmailCodeRequested(result.value.resendAfterSeconds))
        }
    }

    override suspend fun verifyEmailCode(
        email: String,
        code: Secret<String>,
    ): AccountResult<Account> = mutex.withLock {
        val body = VazieApiClient.json.encodeToJsonElement(
            EmailVerifyRequestDto.serializer(),
            EmailVerifyRequestDto(
                email = email.trim(),
                code = code.expose(),
                platform = PLATFORM,
                deviceName = deviceName,
            ),
        )
        when (val result = api.post(PATH_EMAIL_VERIFY, body, EmailVerifyResponseDto.serializer())) {
            is ApiResult.Failure -> AccountResult.Failure(reason(result.failure))
            is ApiResult.Success -> {
                val signedIn = result.value
                tokens.store(Secret.of(signedIn.token))
                // What the verify response already says, so a `/me` that cannot be reached right
                // now still leaves a signed-in person with their address on screen.
                val known = Account(
                    id = signedIn.accountId,
                    email = signedIn.email,
                    status = null,
                    entitlements = emptyList(),
                )
                when (val state = refreshLocked(known)) {
                    is AccountState.SignedIn -> AccountResult.Success(state.account)
                    is AccountState.Offline -> AccountResult.Success(state.account ?: known)
                    // `/me` rejected a token issued a moment ago. Nothing to show but the truth.
                    AccountState.SignedOut, AccountState.Unknown ->
                        AccountResult.Failure(AccountFailure.SessionExpired)
                }
            }
        }
    }

    override suspend fun signOut(): AccountResult<Unit> = mutex.withLock {
        if (tokens.token() == null) {
            forgetLocked()
            return@withLock AccountResult.Success(Unit)
        }
        when (val result = api.delete(PATH_SESSION)) {
            is ApiResult.Success -> {
                forgetLocked()
                AccountResult.Success(Unit)
            }
            is ApiResult.Failure -> when (val reason = reason(result.failure)) {
                // The backend no longer knows this session: it has ended, which is what was asked.
                AccountFailure.SessionExpired -> {
                    forgetLocked()
                    AccountResult.Success(Unit)
                }
                else -> AccountResult.Failure(reason)
            }
        }
    }

    override suspend fun signOutLocally() = mutex.withLock { forgetLocked() }

    override suspend fun deleteAccount(): AccountResult<Unit> = mutex.withLock {
        if (tokens.token() == null) return@withLock AccountResult.Failure(AccountFailure.SessionExpired)
        val body = VazieApiClient.json.encodeToJsonElement(
            DeleteAccountRequestDto.serializer(),
            DeleteAccountRequestDto(confirm = DELETE_CONFIRMATION),
        )
        when (val result = api.delete(PATH_ACCOUNT, body)) {
            is ApiResult.Success -> {
                forgetLocked()
                AccountResult.Success(Unit)
            }
            is ApiResult.Failure -> {
                val reason = reason(result.failure)
                // A session the backend no longer knows cannot delete anything: it is forgotten, and
                // the person signs in again to try.
                if (reason == AccountFailure.SessionExpired) forgetLocked()
                AccountResult.Failure(reason)
            }
        }
    }

    private suspend fun refreshLocked(known: Account? = null): AccountState {
        if (tokens.token() == null) return publish(AccountState.SignedOut)
        return when (val result = api.get(PATH_ME, MeResponseDto.serializer())) {
            is ApiResult.Success -> publish(AccountState.SignedIn(result.value.toAccount()))
            is ApiResult.Failure -> when (reason(result.failure)) {
                AccountFailure.SessionExpired -> {
                    tokens.clear()
                    publish(AccountState.SignedOut)
                }
                else -> publish(AccountState.Offline(known ?: lastKnownAccount()))
            }
        }
    }

    private suspend fun forgetLocked() {
        tokens.clear()
        publish(AccountState.SignedOut)
    }

    private fun lastKnownAccount(): Account? = when (val current = _state.value) {
        is AccountState.SignedIn -> current.account
        is AccountState.Offline -> current.account
        AccountState.SignedOut, AccountState.Unknown -> null
    }

    private fun publish(state: AccountState): AccountState {
        _state.value = state
        return state
    }

    private fun reason(failure: ApiFailure): AccountFailure = accountFailureOf(failure)

    private fun MeResponseDto.toAccount(): Account = Account(
        id = accountId,
        email = email,
        status = status,
        entitlements = entitlements.map { it.key },
        // VPN Plus is the managed-server entitlement; its end date is the access's end date.
        plus = entitlements.firstOrNull { it.key == PLUS_ENTITLEMENT }?.let { PlusAccess(validUntil = it.validUntil) },
    )

    private companion object {
        const val PATH_EMAIL_START = "/auth/email/start"
        const val PATH_EMAIL_VERIFY = "/auth/email/verify"
        const val PATH_ME = "/me"
        const val PATH_SESSION = "/session"
        const val PATH_ACCOUNT = "/account"

        /** The backend's `DeleteAccountRequest.CONFIRMATION`. */
        const val DELETE_CONFIRMATION = "DELETE"

        /** The backend's `EntitlementKey` VPN Plus grants. */
        const val PLUS_ENTITLEMENT = "MANAGED_SERVER_ACCESS"

        /** The backend's `DevicePlatform.ANDROID`. */
        const val PLATFORM = "ANDROID"

    }
}
