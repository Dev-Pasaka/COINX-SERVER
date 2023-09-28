package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data  class SignInResponse(
    val message :String = "Wrong email or password",
    val status:Boolean = false
)