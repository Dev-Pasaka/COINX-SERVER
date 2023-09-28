package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class UserDataResponse(
    val message:String = "Failed to fetch userdata",

    val status:Boolean = false
)
