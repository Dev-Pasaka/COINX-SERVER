package online.pasaka.dto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoSellAdOrderDto(
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val margin:Double,
    val minLimit:Double,
    val maxLimit:Double,
)
