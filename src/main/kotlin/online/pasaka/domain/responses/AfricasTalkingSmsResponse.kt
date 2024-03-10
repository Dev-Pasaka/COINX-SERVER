package online.pasaka.domain.responses

import io.ktor.http.*
import kotlinx.serialization.Serializable


@Serializable
data class AfricasTalkingSmsResponse(
    val httpStatusCode:Int = HttpStatusCode.OK.value,
    val serverStatus:Boolean = true,
    val errorMessage:String? = null
)