package app.vazie.vpn.data.account

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// The backend's own contracts (VazieBackend `http/api`), field for field. Unknown fields are ignored by
// `VazieApiClient.json`, so the backend may add to them without breaking this client.

@Serializable
internal data class EmailStartRequestDto(val email: String) {
    override fun toString(): String = "EmailStartRequestDto(***)"
}

@Serializable
internal data class EmailStartResponseDto(val resendAfterSeconds: Long)

@Serializable
internal data class EmailVerifyRequestDto(
    val email: String,
    val code: String,
    val platform: String,
    val deviceName: String? = null,
) {
    override fun toString(): String = "EmailVerifyRequestDto(platform=$platform)"
}

@Serializable
internal data class EmailVerifyResponseDto(
    val accountId: String,
    val deviceId: String,
    val token: String,
    val expiresAt: String,
    val email: String,
    val accountCreated: Boolean,
) {
    override fun toString(): String = "EmailVerifyResponseDto(accountCreated=$accountCreated)"
}

@Serializable
internal data class MeResponseDto(
    val accountId: String,
    val status: String,
    val createdAt: String,
    val email: String? = null,
    val device: MeDeviceDto? = null,
    val entitlements: List<EntitlementDto> = emptyList(),
) {
    override fun toString(): String = "MeResponseDto(status=$status, entitlements=$entitlements)"
}

@Serializable
internal data class MeDeviceDto(
    val id: String,
    val platform: String,
    val displayName: String? = null,
    val createdAt: String,
)

@Serializable
internal data class EntitlementDto(
    val key: String,
    val validUntil: String? = null,
)

/** `DELETE /account`: [confirm] is the backend's fixed confirmation word. */
@Serializable
internal data class DeleteAccountRequestDto(val confirm: String)

/** `GET /plans?product=vpn`: the plans on sale with their prices, and how they are bought right now. Each plan
 * is kept as raw JSON and read on its own, so one plan this build cannot read leaves the others standing. */
@Serializable
internal data class PlansResponseDto(
    val plans: List<JsonElement> = emptyList(),
    val purchase: PurchaseDto? = null,
)

/** `PROVIDER` — the payment page; `CONTACT` — write to the author at [contactUrl]. */
@Serializable
internal data class PurchaseDto(val mode: String, val contactUrl: String? = null)

/** `GET /subscription`. */
@Serializable
internal data class SubscriptionResponseDto(
    val plan: String,
    val planDisplayName: String,
    val state: String,
    val expiresAt: String? = null,
    /** What the account holds now, bought or granted; a grant shows here and not in [plan]. */
    val entitlements: List<EntitlementDto> = emptyList(),
)
