package app.vazie.vpn.feature.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.core.analytics.Diagnostics
import app.vazie.vpn.core.analytics.PaymentEvent
import app.vazie.vpn.core.analytics.PaymentStage
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Paying for VPN Plus: make sure somebody is signed in, send the person to the site on the plan they chose, and
 * when they are back ask the backend, a few times, whether VPN Plus is on. The site takes the payment and the
 * backend grants access when the payment service confirms it; nothing here does either. */
@HiltViewModel
class PlusPaymentViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val account: AccountRepository,
    /** How payments end — only with the person's consent, see [Diagnostics]. */
    private val diagnostics: Diagnostics,
) : ViewModel() {

    /** A plan code from the catalogue, as the plan screen chose it. */
    private val planCode: String = savedState.toRoute<PlusPaymentRoute>().plan

    private val _state = MutableStateFlow<PlusPaymentUiState>(
        if (savedState.get<Boolean>(KEY_OPENED) == true) PlusPaymentUiState.Awaiting() else PlusPaymentUiState.Opening,
    )
    val state: StateFlow<PlusPaymentUiState> = _state.asStateFlow()

    private val _effects = Channel<PlusPaymentEffect>(Channel.BUFFERED)
    val effects: Flow<PlusPaymentEffect> = _effects.receiveAsFlow()

    private var check: Job? = null

    init {
        if (_state.value == PlusPaymentUiState.Opening) open()
    }

    fun onAction(action: PlusPaymentAction) {
        when (action) {
            PlusPaymentAction.Resumed -> if (isWaitingIdle()) check()
            PlusPaymentAction.Check -> if (_state.value is PlusPaymentUiState.Awaiting) check()
            PlusPaymentAction.OpenSiteAgain -> {
                if (_state.value == PlusPaymentUiState.SiteNotOpened) _state.value = PlusPaymentUiState.Opening
                _effects.trySend(PlusPaymentEffect.OpenSite(planCode))
            }
            PlusPaymentAction.SiteOpened -> siteOpened()
            PlusPaymentAction.SiteNotOpened -> {
                check?.cancel()
                _state.value = PlusPaymentUiState.SiteNotOpened
            }
            PlusPaymentAction.Done -> _effects.trySend(PlusPaymentEffect.Finished)
        }
    }

    private fun isWaitingIdle(): Boolean = (_state.value as? PlusPaymentUiState.Awaiting)?.checking == false

    private fun open() {
        viewModelScope.launch {
            val current = account.state.value
            val signedIn = when (current) {
                is AccountState.SignedIn -> true
                AccountState.SignedOut -> false
                is AccountState.Offline -> current.account != null
                AccountState.Unknown -> account.refresh().let { it is AccountState.SignedIn || it is AccountState.Offline }
            }
            if (!signedIn) {
                _effects.send(PlusPaymentEffect.SignInFirst)
                return@launch
            }
            // What the account holds now, so a second period bought for the same account is told from the one
            // it already has.
            val had = plusOf(account.state.value)
            savedState[KEY_HAD_PLUS] = had != null
            savedState[KEY_VALID_UNTIL] = had?.validUntil
            _effects.send(PlusPaymentEffect.OpenSite(planCode))
        }
    }

    private fun siteOpened() {
        if (savedState.get<Boolean>(KEY_OPENED) != true) {
            savedState[KEY_OPENED] = true
            diagnostics.payment(PaymentEvent(PaymentStage.OPENED, planCode))
        }
        _state.value = PlusPaymentUiState.Awaiting()
    }

    /** The person is back from the site: ask the backend, a few times, because the payment service tells it a
     * moment after the person sees "paid". */
    private fun check() {
        check?.cancel()
        check = viewModelScope.launch {
            _state.value = PlusPaymentUiState.Awaiting(checking = true)
            for (attempt in 1..CHECKS) {
                val answer = account.refresh()
                if (answer == AccountState.SignedOut) {
                    savedState.remove<Boolean>(KEY_OPENED)
                    _effects.send(PlusPaymentEffect.SignInFirst)
                    return@launch
                }
                if (activated(answer)) {
                    diagnostics.payment(PaymentEvent(PaymentStage.SUCCEEDED, planCode))
                    _state.value = PlusPaymentUiState.Activated
                    return@launch
                }
                if (attempt < CHECKS) delay(INTERVAL_MILLIS)
            }
            _state.value = PlusPaymentUiState.Awaiting(notSeen = true)
        }
    }

    /** VPN Plus is on, and is not the access the account held before the site was opened. */
    private fun activated(answer: AccountState): Boolean {
        val now = plusOf(answer) ?: return false
        val hadPlus = savedState.get<Boolean>(KEY_HAD_PLUS) == true
        return !hadPlus || now.validUntil != savedState.get<String>(KEY_VALID_UNTIL)
    }

    private fun plusOf(state: AccountState): PlusAccess? = when (state) {
        is AccountState.SignedIn -> state.account.plus
        is AccountState.Offline -> state.account?.plus
        AccountState.SignedOut, AccountState.Unknown -> null
    }

    private companion object {
        const val KEY_OPENED = "plus.payment.opened"
        const val KEY_HAD_PLUS = "plus.payment.hadPlus"
        const val KEY_VALID_UNTIL = "plus.payment.validUntil"
        const val CHECKS = 6
        const val INTERVAL_MILLIS = 5_000L
    }
}
