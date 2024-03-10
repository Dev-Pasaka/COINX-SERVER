package online.pasaka.domain.responses

import kotlinx.serialization.Serializable

@Serializable
data class PasswordResetResponse(
    val status:Boolean,
    val message:String
)
