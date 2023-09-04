package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable

@Serializable
data class MerchantFloatWithdrawalMessage(
    val email:String,
    val amount:Double,
    val currency:String
)
