package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory

@Serializable
data class MerchantFloatTopUpTransactionsHistoryResponse(
    val status:Boolean = false,
    val body:List<online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory>
)
