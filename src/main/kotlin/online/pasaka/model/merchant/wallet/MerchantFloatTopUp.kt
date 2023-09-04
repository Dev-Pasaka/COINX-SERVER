package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable
import java.util.Currency
@Serializable
data class MerchantTopUp(
    val amount:Double,
    val currency: String,
)