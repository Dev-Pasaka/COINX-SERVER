package online.pasaka.dto

import kotlinx.serialization.Serializable

@Serializable
data class SellOrderDto(
    val adId: String,
    val cryptoName: String,
    val cryptoSymbol: String,
    val cryptoAmount: Double,
)
