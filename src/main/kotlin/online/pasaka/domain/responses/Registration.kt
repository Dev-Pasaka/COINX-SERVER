package online.pasaka.domain.responses

import kotlinx.serialization.Serializable

@Serializable
data class Registration(

    val message:String = "failed to register",

    val isRegistered:Boolean = false

)
