package online.pasaka.domain.responses

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePasswordResponse(
        val status: Boolean = false,
        val message: String = "Failed to update password",
)
