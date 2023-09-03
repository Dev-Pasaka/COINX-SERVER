package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class PhoneQuery(
    val status: Boolean = false,
    val message: String = "Phone number not found"
)
