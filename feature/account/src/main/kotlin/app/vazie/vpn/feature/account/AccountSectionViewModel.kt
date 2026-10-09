package app.vazie.vpn.feature.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.PlusAccess
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** The account block at the top of Settings: who is signed in, and signing out. */
@HiltViewModel
class AccountSectionViewModel @Inject constructor(
    private val account: AccountRepository,
) : ViewModel() {

    private val progress = MutableStateFlow(Progress())

    val state: StateFlow<AccountSectionUiState> = combine(account.state, progress) { state, progress ->
        val ui = state.toUi()
        AccountSectionUiState(
            account = ui,
            busy = progress.busy,
            offerLocalSignOut = progress.offerLocalSignOut,
            deleteConfirmation = progress.deleteConfirmation,
            deleteReady = confirmsDeletion(ui, progress.deleteConfirmation),
            deleteFailed = progress.deleteFailed,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = AccountSectionUiState(account = account.state.value.toUi()),
    )

    fun onAction(action: AccountSectionAction) {
        when (action) {
            AccountSectionAction.SignOut -> signOut()
            AccountSectionAction.ConfirmLocalSignOut -> signOutLocally()
            AccountSectionAction.DismissLocalSignOut ->
                progress.update { it.copy(offerLocalSignOut = false) }
            AccountSectionAction.Retry -> refresh()
            is AccountSectionAction.DeleteConfirmationChanged ->
                progress.update { it.copy(deleteConfirmation = action.text, deleteFailed = false) }
            AccountSectionAction.ConfirmDelete -> deleteAccount()
            // Navigation belongs to the route; nothing to do here.
            AccountSectionAction.SignIn,
            AccountSectionAction.OpenPlus,
            AccountSectionAction.ManagePlus,
            AccountSectionAction.OpenAccount,
            AccountSectionAction.OpenDeleteAccount,
            -> Unit
        }
    }

    private fun signOut() {
        if (progress.value.busy) return
        progress.update { it.copy(busy = true) }
        viewModelScope.launch {
            val result = account.signOut()
            progress.update {
                Progress(busy = false, offerLocalSignOut = result is AccountResult.Failure)
            }
        }
    }

    private fun signOutLocally() {
        progress.update { it.copy(busy = true, offerLocalSignOut = false) }
        viewModelScope.launch {
            account.signOutLocally()
            progress.update { Progress() }
        }
    }

    /** Deletes the account once its address has been typed; success signs out, failure keeps the session
     * and says so. */
    private fun deleteAccount() {
        val confirmed = confirmsDeletion(account.state.value.toUi(), progress.value.deleteConfirmation)
        if (progress.value.busy || !confirmed) return
        progress.update { it.copy(busy = true, deleteFailed = false) }
        viewModelScope.launch {
            val result = account.deleteAccount()
            progress.update { Progress(deleteFailed = result is AccountResult.Failure) }
        }
    }

    private fun refresh() {
        if (progress.value.busy) return
        progress.update { it.copy(busy = true) }
        viewModelScope.launch {
            account.refresh()
            progress.update { it.copy(busy = false) }
        }
    }

    private data class Progress(
        val busy: Boolean = false,
        val offerLocalSignOut: Boolean = false,
        val deleteConfirmation: String = "",
        val deleteFailed: Boolean = false,
    )

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

@Immutable
data class AccountSectionUiState(
    val account: AccountUi = AccountUi.Checking,
    val busy: Boolean = false,
    val offerLocalSignOut: Boolean = false,
    /** What has been typed to confirm the deletion. */
    val deleteConfirmation: String = "",
    /** The typed text is the account's address, so deleting is allowed. */
    val deleteReady: Boolean = false,
    /** The last deletion did not happen; the account is intact. */
    val deleteFailed: Boolean = false,
) {
    override fun toString(): String = "AccountSectionUiState(account=$account, busy=$busy, deleteReady=$deleteReady)"
}

/** What the block shows. */
@Immutable
sealed interface AccountUi {
    data object Checking : AccountUi
    data object SignedOut : AccountUi
    data class SignedIn(val email: String?, val status: String?, val plus: PlusAccess? = null) : AccountUi {
        override fun toString(): String = "SignedIn(status=$status, plus=${plus != null})"
    }
    data class Offline(val email: String?, val plus: PlusAccess? = null) : AccountUi {
        override fun toString(): String = "Offline"
    }
}

sealed interface AccountSectionAction {
    data object SignIn : AccountSectionAction
    data object SignOut : AccountSectionAction
    data object ConfirmLocalSignOut : AccountSectionAction
    data object DismissLocalSignOut : AccountSectionAction
    data object Retry : AccountSectionAction

    /** Open the separate deletion screen; handled by navigation. */
    data object OpenDeleteAccount : AccountSectionAction

    /** The address typed to confirm the deletion. */
    data class DeleteConfirmationChanged(val text: String) : AccountSectionAction {
        override fun toString(): String = "DeleteConfirmationChanged"
    }

    /** Delete; ignored unless the typed address matches the account's. */
    data object ConfirmDelete : AccountSectionAction

    /** Open the VPN Plus plans; handled by navigation. */
    data object OpenPlus : AccountSectionAction

    /** Open the active VPN Plus subscription; handled by navigation. */
    data object ManagePlus : AccountSectionAction

    /** Open the account screen; handled by navigation. */
    data object OpenAccount : AccountSectionAction
}

/** Deleting is confirmed by typing the account's own address; case and surrounding spaces do not matter. */
internal fun confirmsDeletion(account: AccountUi, typed: String): Boolean {
    val email = (account as? AccountUi.SignedIn)?.email ?: return false
    return typed.trim().equals(email.trim(), ignoreCase = true)
}

internal fun AccountState.toUi(): AccountUi = when (this) {
    AccountState.Unknown -> AccountUi.Checking
    AccountState.SignedOut -> AccountUi.SignedOut
    is AccountState.SignedIn -> AccountUi.SignedIn(email = account.email, status = account.status, plus = account.plus)
    is AccountState.Offline -> AccountUi.Offline(email = account?.email, plus = account?.plus)
}
