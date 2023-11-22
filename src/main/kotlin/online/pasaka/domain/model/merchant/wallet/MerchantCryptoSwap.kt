package online.pasaka.domain.model.merchant.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MerchantCryptoSwap(
    val cryptoAmount: Double,
    val fromCryptoSymbol: String,
    val toCryptoSymbol: String
)
