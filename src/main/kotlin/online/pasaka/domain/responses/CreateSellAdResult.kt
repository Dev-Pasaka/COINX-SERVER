package online.pasaka.domain.responses

import kotlinx.serialization.Serializable

@Serializable
data class CreateSellAdResult(
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
    val message: DefaultResponse
)