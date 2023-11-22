package online.pasaka.domain.responses

import kotlinx.serialization.Serializable

@Serializable
data class MerchantRegistrationResponse(
    val status: Boolean = false,
    val message: String = "Merchant verification failed"
)
