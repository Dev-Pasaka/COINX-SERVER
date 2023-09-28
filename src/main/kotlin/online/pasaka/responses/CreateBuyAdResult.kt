package online.pasaka.responses

import kotlinx.serialization.Serializable

@Serializable
data class CreateBuyAdResult(
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
    val message:DefaultResponse
)
