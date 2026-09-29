package app.vazie.vpn.data.account

import app.vazie.vpn.account.api.AccountFailure
import app.vazie.vpn.account.api.AccountResult
import app.vazie.vpn.account.api.PaymentStart
import app.vazie.vpn.account.api.PlusPaymentRepository
import app.vazie.vpn.account.api.PlusPlan
import app.vazie.vpn.account.api.PlusPurchase
import app.vazie.vpn.core.network.ApiResult
import app.vazie.vpn.core.network.VazieApiClient

/** Opens VPN Plus payments on Vazie's backend: `POST /payments` with the plan's code and an
 * `Idempotency-Key`, answered with a single-use link to the payment page. */
internal class VaziePlusPaymentRepository(
    private val api: VazieApiClient,
) : PlusPaymentRepository {

    override suspend fun startPayment(plan: PlusPlan, idempotencyKey: String): AccountResult<PaymentStart> {
        val body = VazieApiClient.json.encodeToJsonElement(
            CreatePaymentRequestDto.serializer(),
            CreatePaymentRequestDto(plan = plan.code),
        )
        return when (
            val result = api.post(
                PATH_PAYMENTS,
                body,
                PaymentResponseDto.serializer(),
                headers = mapOf(IDEMPOTENCY_HEADER to idempotencyKey),
            )
        ) {
            is ApiResult.Success -> {
                val url = result.value.confirmationUrl
                // A payment without a link to complete it is one nobody can pay; say so rather than
                // opening an empty page.
                if (url.isNullOrBlank() || !url.startsWith(HTTPS)) {
                    AccountResult.Failure(AccountFailure.Unknown)
                } else {
                    AccountResult.Success(PaymentStart(paymentId = result.value.id, confirmationUrl = url))
                }
            }
            is ApiResult.Failure -> AccountResult.Failure(accountFailureOf(result.failure))
        }
    }

    override suspend fun purchase(): PlusPurchase =
        when (val result = api.get(PATH_PLANS, PlansResponseDto.serializer())) {
            is ApiResult.Failure -> PlusPurchase.PaymentPage
            is ApiResult.Success -> {
                val purchase = result.value.purchase
                val url = purchase?.contactUrl
                if (purchase?.mode == MODE_CONTACT && url != null && url.startsWith(HTTPS)) {
                    PlusPurchase.Contact(url)
                } else {
                    PlusPurchase.PaymentPage
                }
            }
        }

    private companion object {
        const val PATH_PLANS = "/plans?product=vpn"
        const val MODE_CONTACT = "CONTACT"
        const val PATH_PAYMENTS = "/payments"
        const val IDEMPOTENCY_HEADER = "Idempotency-Key"
        const val HTTPS = "https://"
    }
}
