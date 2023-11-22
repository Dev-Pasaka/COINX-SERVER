package online.pasaka.domain.responses

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class DefaultResponse(

    @Contextual
    val message:String = "",
    val status:Boolean = false

)
