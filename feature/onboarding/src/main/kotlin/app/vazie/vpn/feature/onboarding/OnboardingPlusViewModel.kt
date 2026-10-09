package app.vazie.vpn.feature.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vazie.vpn.account.api.PlusCatalogRepository
import app.vazie.vpn.account.api.PlusCatalogState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The VPN Plus plans for the last onboarding page, read from the backend when onboarding opens. Nothing on the
 * earlier pages waits for them. */
@HiltViewModel
class OnboardingPlusViewModel @Inject constructor(
    private val catalog: PlusCatalogRepository,
) : ViewModel() {

    val state: StateFlow<PlusCatalogState> = catalog.state

    init {
        retry()
    }

    fun retry() {
        viewModelScope.launch { catalog.refresh() }
    }
}
