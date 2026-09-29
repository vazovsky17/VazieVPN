package app.vazie.vpn.feature.account

import app.vazie.vpn.account.api.Account
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.EmailCodeRequested
import app.vazie.vpn.core.model.Secret
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** An [AccountRepository] with scripted answers that records calls; transitions mirror the real one. */
internal class FakeAccountRepository(
    initial: AccountState = AccountState.SignedOut,
) : AccountRepository {

    private val _state = MutableStateFlow(initial)
    override val state: StateFlow<AccountState> = _state

    var requestResult: AccountResult<EmailCodeRequested> = AccountResult.Success(EmailCodeRequested(60))
    var verifyResult: AccountResult<Account> = AccountResult.Success(ACCOUNT)
    var signOutResult: AccountResult<Unit> = AccountResult.Success(Unit)
    var refreshResult: AccountState = AccountState.SignedIn(ACCOUNT)

    val requestedEmails = mutableListOf<String>()
    val verifiedCodes = mutableListOf<String>()
    var signOuts = 0
    var localSignOuts = 0
    var refreshes = 0

    override suspend fun refresh(): AccountState {
        refreshes++
        _state.value = refreshResult
        return refreshResult
    }

    override suspend fun requestEmailCode(email: String): AccountResult<EmailCodeRequested> {
        requestedEmails += email
        return requestResult
    }

    override suspend fun verifyEmailCode(email: String, code: Secret<String>): AccountResult<Account> {
        verifiedCodes += code.expose()
        val result = verifyResult
        if (result is AccountResult.Success) _state.value = AccountState.SignedIn(result.value)
        return result
    }

    override suspend fun signOut(): AccountResult<Unit> {
        signOuts++
        val result = signOutResult
        if (result is AccountResult.Success) _state.value = AccountState.SignedOut
        return result
    }

    override suspend fun signOutLocally() {
        localSignOuts++
        _state.value = AccountState.SignedOut
    }

    var deleteResult: AccountResult<Unit> = AccountResult.Success(Unit)
    var deletions = 0

    override suspend fun deleteAccount(): AccountResult<Unit> {
        deletions++
        val result = deleteResult
        if (result is AccountResult.Success) _state.value = AccountState.SignedOut
        return result
    }

    companion object {
        val ACCOUNT = Account(
            id = "00000000-0000-4000-8000-00000000abcd",
            email = "person@example.com",
            status = "ACTIVE",
            entitlements = emptyList(),
        )
    }
}
