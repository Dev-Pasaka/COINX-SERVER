package online.pasaka.model.merchant.wallet.crypto

import kotlinx.serialization.Serializable

@Serializable
data class CryptoCoin(
    val symbol:String,
    val name: String,
    val amount: Double,
)
