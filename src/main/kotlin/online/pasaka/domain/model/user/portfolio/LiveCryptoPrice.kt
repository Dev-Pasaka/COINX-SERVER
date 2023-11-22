package online.pasaka.domain.model.user.portfolio

import kotlinx.serialization.Serializable

@Serializable
data class LiveCryptoPrice(
    val symbol:String,
    val name: String,
    var amount: Double,
    var marketPrice: Double
)
