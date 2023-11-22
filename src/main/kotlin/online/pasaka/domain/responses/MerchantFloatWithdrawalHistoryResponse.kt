package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory

@Serializable
data class MerchantFloatWithdrawalHistoryResponse(
    val status: Boolean = false,
    val body: List<online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory>
)
