package online.pasaka.responses

import com.mongodb.client.FindIterable
import kotlinx.serialization.Serializable
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory

@Serializable
data class MerchantFloatTransactionsHistoryResponse(
    val status:Boolean = false,
    val body:List<MerchantTopUpsHistory>
)
