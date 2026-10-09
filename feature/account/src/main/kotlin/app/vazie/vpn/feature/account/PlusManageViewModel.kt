package app.vazie.vpn.feature.account

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PlusSubscription
import app.vazie.vpn.account.api.PlusSubscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** What the subscription screen shows. */
@Immutable
sealed interface PlusManageUiState {
    data object Loading : PlusManageUiState
    data class Loaded(val subscription: PlusSubscription) : PlusManageUiState
    data class Problem(val failure: AccountFailure) : PlusManageUiState
}

/** The active VPN Plus subscription: reads it from the backend, and refreshes the account alongside so the
 * Settings row agrees with what this screen says. */
@HiltViewModel
class PlusManageViewModel @Inject constructor(
    private val subscriptions: PlusSubscriptionRepository,
    private val account: AccountRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<PlusManageUiState>(PlusManageUiState.Loading)
    val state: StateFlow<PlusManageUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _state.value = PlusManageUiState.Loading
        viewModelScope.launch { account.refresh() }
        viewModelScope.launch {
            _state.value = when (val result = subscriptions.current()) {
                is AccountResult.Success -> PlusManageUiState.Loaded(result.value)
                is AccountResult.Failure -> PlusManageUiState.Problem(result.reason)
            }
        }
    }
}
