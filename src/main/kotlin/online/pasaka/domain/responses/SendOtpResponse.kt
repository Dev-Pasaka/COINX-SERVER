package online.pasaka.domain.responses

import kotlinx.serialization.Serializable

@Serializable
data class SendOtpResponse(
    val status:Boolean = false,
    val message:String = "Wrong parameters",
    val otpCode:String? = null
)
