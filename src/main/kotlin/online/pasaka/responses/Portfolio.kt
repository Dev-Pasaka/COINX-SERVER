package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class Portfolio(

    val message:String = "failed to your portfolio",

    val status:Boolean = false

)