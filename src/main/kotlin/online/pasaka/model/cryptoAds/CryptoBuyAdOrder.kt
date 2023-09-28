package online.pasaka.model.cryptoAds

import kotlinx.serialization.Serializable

data class CryptoBuyAdOrder(
    val email:String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val margin:Double,
    val minLimit:Double,
    val maxLimit:Double,
)