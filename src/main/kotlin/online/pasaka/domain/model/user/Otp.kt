package online.pasaka.domain.model.user

import kotlinx.serialization.Serializable

@Serializable
data class Otp(
    val toPhone:String,
    val otpCode:String,
)
