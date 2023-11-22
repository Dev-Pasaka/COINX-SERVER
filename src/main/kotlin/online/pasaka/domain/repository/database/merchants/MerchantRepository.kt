package online.pasaka.domain.repository.database.merchants

import online.pasaka.domain.dto.merchant.MerchantDataDto
import online.pasaka.domain.model.merchant.Merchant
import online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory

interface MerchantRepository {

    suspend fun getMerchantFloatTopUpHistory(email: String, allHistory:String): List<MerchantTopUpsHistory>?
    suspend fun getMerchantFloatWithdrawalHistory(email: String, allHistory:String): List<MerchantsWithdrawalsHistory>?
    suspend fun getMerchantData(email: String): MerchantDataDto?
}