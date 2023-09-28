package online.pasaka.dto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoBuyAdOrderDto(
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val margin:Double,
    val minLimit:Double,
    val maxLimit:Double,
)