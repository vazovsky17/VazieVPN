package app.vazie.vpn.feature.account

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountRepository
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.AccountState
import app.vazie.vpn.account.api.PlusPaymentRepository
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPurchase
import app.vazie.vpn.core.analytics.Diagnostics
import app.vazie.vpn.core.analytics.PaymentEvent
import app.vazie.vpn.core.analytics.PaymentStage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/** Paying for VPN Plus: make sure somebody is signed in, open the payment on the backend, show its page, and
 * read how it ended. */
@HiltViewModel
class PlusPaymentViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val account: AccountRepository,
    private val payments: PlusPaymentRepository,
    /** How payments end — only with the person's consent, see [Diagnostics]. */
    private val diagnostics: Diagnostics,
) : ViewModel() {

    private val plan: PlusPlan = planOf(savedState.toRoute<PlusPaymentRoute>().plan)

    private val _state = MutableStateFlow<PlusPaymentUiState>(PlusPaymentUiState.Opening)
    val state: StateFlow<PlusPaymentUiState> = _state.asStateFlow()

    private val _effects = Channel<PlusPaymentEffect>(Channel.BUFFERED)
    val effects: Flow<PlusPaymentEffect> = _effects.receiveAsFlow()

    init {
        open()
    }

    fun onAction(action: PlusPaymentAction) {
        when (action) {
            is PlusPaymentAction.PageFinished -> when (action.outcome) {
                PaymentOutcome.Sent -> {
                    report(PaymentStage.SUCCEEDED)
                    _state.value = PlusPaymentUiState.Sent
                    viewModelScope.launch { account.refresh() }
                }
                PaymentOutcome.NotCompleted -> {
                    report(PaymentStage.NOT_COMPLETED)
                    _state.value = PlusPaymentUiState.NotCompleted
                }
            }
            PlusPaymentAction.Closed -> if (_state.value is PlusPaymentUiState.Paying) {
                report(PaymentStage.CLOSED)
                _state.value = PlusPaymentUiState.NotCompleted
            }
            PlusPaymentAction.Retry -> {
                // A payment that did not go through is over; trying again is a new purchase.
                if (_state.value == PlusPaymentUiState.NotCompleted) savedState.remove<String>(KEY_IDEMPOTENCY)
                open()
            }
            PlusPaymentAction.Done -> _effects.trySend(PlusPaymentEffect.Finished)
        }
    }

    private fun open() {
        _state.value = PlusPaymentUiState.Opening
        viewModelScope.launch {
            val signedIn = when (val current = account.state.value) {
                is AccountState.SignedIn -> true
                AccountState.SignedOut -> false
                is AccountState.Offline -> current.account != null
                AccountState.Unknown -> account.refresh().let { it is AccountState.SignedIn || it is AccountState.Offline }
            }
            if (!signedIn) {
                _effects.send(PlusPaymentEffect.SignInFirst)
                return@launch
            }
            // While the backend takes no payments, the author is written to instead; the sign-in
            // above still comes first, because access is switched on for an account.
            val purchase = payments.purchase()
            if (purchase is PlusPurchase.Contact) {
                _state.value = PlusPaymentUiState.ByContact(
                    plan = plan,
                    contactUrl = purchase.contactUrl,
                    contactLabel = purchase.contactLabel,
                    email = signedInEmail(),
                )
                return@launch
            }
            when (val result = payments.startPayment(plan, idempotencyKey())) {
                is AccountResult.Success -> {
                    report(PaymentStage.OPENED)
                    _state.value = PlusPaymentUiState.Paying(result.value.confirmationUrl)
                }
                is AccountResult.Failure -> if (result.reason == AccountFailure.SessionExpired) {
                    account.signOutLocally()
                    _effects.send(PlusPaymentEffect.SignInFirst)
                } else {
                    report(PaymentStage.FAILED, reason = result.reason::class.simpleName)
                    _state.value = PlusPaymentUiState.Problem(result.reason)
                }
            }
        }
    }

    private fun signedInEmail(): String? = when (val current = account.state.value) {
        is AccountState.SignedIn -> current.account.email
        is AccountState.Offline -> current.account?.email
        AccountState.SignedOut, AccountState.Unknown -> null
    }

    private fun report(stage: PaymentStage, reason: String? = null) =
        diagnostics.payment(PaymentEvent(stage = stage, plan = plan.name, reason = reason))

    private fun idempotencyKey(): String =
        savedState.get<String>(KEY_IDEMPOTENCY) ?: UUID.randomUUID().toString().also { savedState[KEY_IDEMPOTENCY] = it }

    private companion object {
        const val KEY_IDEMPOTENCY = "plus.payment.idempotency"
    }
}

/** A plan from its route argument; an unknown one falls back to the month. */
internal fun planOf(name: String): PlusPlan = PlusPlan.entries.firstOrNull { it.name == name } ?: PlusPlan.MONTHLY
