package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PlusAccess
import app.vazie.vpn.account.api.PlusSubscription
import app.vazie.vpn.account.api.PlusSubscriptionRepository
import app.vazie.vpn.core.network.ApiResult
import app.vazie.vpn.core.network.VazieApiClient

/** Reads the signed-in account's subscription from `GET /subscription`. */
internal class VaziePlusSubscriptionRepository(
    private val api: VazieApiClient,
) : PlusSubscriptionRepository {

    override suspend fun current(): AccountResult<PlusSubscription> =
        when (val result = api.get(PATH_SUBSCRIPTION, SubscriptionResponseDto.serializer())) {
            is ApiResult.Success -> AccountResult.Success(
                PlusSubscription(
                    planCode = result.value.plan,
                    planName = result.value.planDisplayName,
                    state = result.value.state,
                    expiresAt = result.value.expiresAt,
                    // As Settings reads VPN Plus from `/me`: the entitlement, so a grant from the admin panel counts.
                    access = result.value.entitlements.firstOrNull { it.key == PLUS_ENTITLEMENT }?.let { PlusAccess(validUntil = it.validUntil) },
                ),
            )
            is ApiResult.Failure -> AccountResult.Failure(accountFailureOf(result.failure))
        }

    private companion object {
        const val PATH_SUBSCRIPTION = "/subscription"

        /** The backend's `EntitlementKey` VPN Plus grants. */
        const val PLUS_ENTITLEMENT = "MANAGED_SERVER_ACCESS"
    }
}
