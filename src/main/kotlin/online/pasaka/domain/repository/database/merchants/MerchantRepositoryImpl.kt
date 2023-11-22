package online.pasaka.domain.repository.database.merchants

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.domain.dto.merchant.MerchantDataDto
import online.pasaka.domain.model.merchant.Merchant
import online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory
import online.pasaka.infrastructure.database.Entries
import org.litote.kmongo.*

class MerchantRepositoryImpl(val entries: Entries = Entries) : MerchantRepository {
    override suspend fun getMerchantFloatTopUpHistory(email: String, allHistory: String): List<MerchantTopUpsHistory>? {
        return coroutineScope {
            when (allHistory) {
                "false" -> {
                    return@coroutineScope try {
                        async(Dispatchers.IO) {
                            entries.dbMerchantTopUpsHistory.find().limit(5).sort(descending())
                                .filter { it.email == email }
                        }.await()
                    }catch (e:Exception){null}
                }

                else -> {
                    return@coroutineScope try {
                        async(Dispatchers.IO) {
                            entries.dbMerchantTopUpsHistory.find()
                                .filter { it.email == email }
                        }.await()
                    }catch (e:Exception){null}
                }
            }
        }
    }

    override suspend fun getMerchantFloatWithdrawalHistory(
        email: String,
        allHistory: String
    ): List<MerchantsWithdrawalsHistory>? {
        return coroutineScope {
            when (allHistory) {
                "false" -> {
                    return@coroutineScope try {
                        async(Dispatchers.IO) {
                            entries.dbMerchantsWithdrawalsHistory.find().limit(5).sort(descending())
                                .filter { it.email == email }
                        }.await()
                    }catch (e:Exception){null}
                }

                else -> {
                    return@coroutineScope try {
                        async(Dispatchers.IO) {
                            entries.dbMerchantsWithdrawalsHistory.find()
                                .filter { it.email == email }
                        }.await()
                    }catch (e:Exception){null}
                }
            }
        }
    }

    override suspend fun getMerchantData(email: String): MerchantDataDto? {
        return coroutineScope {
            return@coroutineScope async(Dispatchers.IO) {
                try {
                    val merchantData = entries.dbMerchant.findOne(Merchant::email eq email)
                    if (merchantData != null){
                        MerchantDataDto(
                            username = merchantData.username,
                            email = merchantData.email,
                            phoneNumber = merchantData.phoneNumber,
                            ordersMade = merchantData.ordersMade,
                            ordersCompleted = merchantData.ordersCompleted,
                            ordersCompletedByPercentage = merchantData.ordersCompletedByPercentage,
                            paymentMethod = merchantData.paymentMethod,
                            lastSeen = merchantData.lastSeen
                        )
                    }else{
                        null
                    }
                }catch (e:Exception){
                    null
                }
            }.await()
        }
    }
}
