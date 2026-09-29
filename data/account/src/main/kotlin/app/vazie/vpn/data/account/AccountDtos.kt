package app.vazie.vpn.data.account

import kotlinx.serialization.Serializable

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

/** `GET /plans`: only how VPN Plus is bought right now is read here; the prices are the app's own. */
@Serializable
internal data class PlansResponseDto(val purchase: PurchaseDto? = null)

/** `PROVIDER` — the payment page; `CONTACT` — write to the author at [contactUrl]. */
@Serializable
internal data class PurchaseDto(val mode: String, val contactUrl: String? = null)

/** `POST /payments`: the plan, by code. The price is the backend's. */
@Serializable
internal data class CreatePaymentRequestDto(val plan: String)

/** The payment the backend opened; [confirmationUrl] is present only on creation. */
@Serializable
internal data class PaymentResponseDto(
    val id: String,
    val status: String,
    val plan: String,
    val confirmationUrl: String? = null,
) {
    override fun toString(): String = "PaymentResponseDto(id=${id.take(8)}…, status=$status, plan=$plan)"
}

/** `GET /subscription`. */
@Serializable
internal data class SubscriptionResponseDto(
    val plan: String,
    val planDisplayName: String,
    val state: String,
    val expiresAt: String? = null,
)
