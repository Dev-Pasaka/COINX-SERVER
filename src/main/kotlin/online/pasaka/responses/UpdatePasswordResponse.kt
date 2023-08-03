package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePassword(
        val status: Boolean = false,
        val message: String = "Failed to update password",
)
