package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class InvalidToken(
    val message:String = "InvalidToken",
    val status:Boolean = false
)
