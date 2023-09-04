package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MerchantFloatTopUp(
    val amount:Double,
    val currency: String,
)