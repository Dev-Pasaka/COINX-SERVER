package online.pasaka.domain.model.wallet.crypto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoPrice(
    val symbol:String,
    val name: String,
    var amount: Double,
    var marketPrice: Double
)