package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MerchantFloatTopUpMessage(
    val email:String,
    val amount:Double,
    val currency:String
)
