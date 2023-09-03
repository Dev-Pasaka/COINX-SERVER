package online.pasaka.responses

import kotlinx.serialization.Serializable
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory

@Serializable
data class MerchantFloatTopUpTransactionsHistoryResponse(
    val status:Boolean = false,
    val body:List<MerchantTopUpsHistory>
)
