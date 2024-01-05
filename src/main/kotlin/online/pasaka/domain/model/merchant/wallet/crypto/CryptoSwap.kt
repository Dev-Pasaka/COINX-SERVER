package online.pasaka.domain.model.merchant.wallet.crypto

data class CryptoSwap(
    val email:String,
    val cryptoAmount:Double,
    val fromCryptoSymbol:String,
    val toCryptoSymbol:String
)