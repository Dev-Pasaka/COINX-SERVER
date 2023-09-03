package online.pasaka.responses

import kotlinx.serialization.Serializable
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.model.merchant.wallet.MerchantsWithdrawalsHistory

@Serializable
data class MerchantFloatWithdrawalHistoryResponse(
    val status: Boolean = false,
    val body: List<MerchantsWithdrawalsHistory>
)
