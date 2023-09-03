package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class MerchantPaymentMethodResponse(
    val status: Boolean = false,
    val message: String = "Payment failed to be added"
)
