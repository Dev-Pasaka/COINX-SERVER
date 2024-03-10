package online.pasaka.domain.dto

import kotlinx.serialization.Serializable
@Serializable
data class ResetPasswordDto(
    val phoneNumber:String,
    val otpCode:String,
    val newPassword:String
) {
}