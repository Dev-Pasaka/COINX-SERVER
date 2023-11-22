package online.pasaka.domain.dto

import kotlinx.serialization.Serializable

@Serializable
data class BuyOrderDto(
    val adId: String,
    val cryptoName: String,
    val cryptoSymbol: String,
    val cryptoAmount: Double,
)