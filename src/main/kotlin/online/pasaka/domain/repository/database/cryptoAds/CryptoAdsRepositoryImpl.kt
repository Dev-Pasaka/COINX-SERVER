package online.pasaka.domain.repository.database.cryptoAds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.domain.dto.cryptoAds.GetBuyAdDto
import online.pasaka.domain.dto.cryptoAds.GetSellAdDto
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.model.cryptoAds.BuyAd
import online.pasaka.domain.model.cryptoAds.SellAd
import online.pasaka.domain.repository.database.merchants.MerchantRepositoryImpl
import online.pasaka.domain.repository.remote.cryptodata.GetCryptoPrice


class CryptoAdsRepositoryImpl(val entries: Entries = Entries) : CryptoAdsRepository {
    override suspend fun getBuyAds(): List<BuyAd> {
        return coroutineScope {

            return@coroutineScope async(Dispatchers.IO) {
                try {
                    entries.buyAd.find().toList()
                } catch (_: Exception) {
                    emptyList<BuyAd>()
                }
            }.await()

        }
    }

    override suspend fun getSellAds(): List<SellAd> {
        return coroutineScope {
            return@coroutineScope async(Dispatchers.IO) {
                try {
                    entries.sellAd.find().toList()
                } catch (_: Exception) {
                    emptyList<SellAd>()
                }
            }.await()
        }
    }

    override suspend fun getBuyAdsData(): List<GetBuyAdDto>? {
        return coroutineScope {
            return@coroutineScope try {

                async(Dispatchers.IO) {
                    val getAllAds = getBuyAds()
                    val cryptoAdResponse = mutableListOf<GetBuyAdDto>()

                    getAllAds.forEach {

                        val getMerchantData = MerchantRepositoryImpl().getMerchantData(email = it.email)
                        val getCryptoPrice = GetCryptoPrice().getCryptoMetadata(cryptoSymbol = it.cryptoSymbol, currency = "KES").price.toString().toDoubleOrNull() ?: 0.0
                        if (getMerchantData != null){
                            cryptoAdResponse.add(
                                GetBuyAdDto(
                                    id = it.id,
                                    merchantUsername = it.merchantUsername,
                                    email = it.email,
                                    cryptoName = it.cryptoName,
                                    cryptoSymbol = it.cryptoSymbol,
                                    totalAmount = it.totalAmount,
                                    minLimit = it.minLimit,
                                    maxLimit = it.maxLimit,
                                    adStatus = it.adStatus,
                                    margin = it.margin,
                                    cryptoPrice = (getCryptoPrice + (getCryptoPrice*it.margin)),
                                    createdAt = it.createdAt,
                                    ordersMade = getMerchantData.ordersMade,
                                    ordersCompleted = getMerchantData.ordersCompleted,
                                    paymentMethod = getMerchantData.paymentMethod,
                                    ordersCompletedByPercentage =getMerchantData.ordersCompletedByPercentage,
                                    lastSeen = getMerchantData.lastSeen
                                )
                            )
                        }

                    }
                    cryptoAdResponse
                }.await()

            }catch(e:Exception){
                null
            }
        }


    }

    override suspend fun getSellAdsData(): List<GetSellAdDto>? {
        return coroutineScope {
            return@coroutineScope try {

                async(Dispatchers.IO) {
                    val getAllAds = getSellAds()
                    val cryptoAdResponse = mutableListOf<GetSellAdDto>()

                    getAllAds.forEach {
                        val getMerchantData = MerchantRepositoryImpl().getMerchantData(email = it.email)
                        val getCryptoPrice = GetCryptoPrice().getCryptoMetadata(cryptoSymbol = it.cryptoSymbol, currency = "KES").price.toString().toDoubleOrNull() ?: 0.0
                        if (getMerchantData != null){
                            cryptoAdResponse.add(
                                GetSellAdDto(
                                    id = it.id,
                                    merchantUsername = it.merchantUsername,
                                    email = it.email,
                                    cryptoName = it.cryptoName,
                                    cryptoSymbol = it.cryptoSymbol,
                                    totalAmount = it.totalAmount,
                                    minLimit = it.minLimit,
                                    maxLimit = it.maxLimit,
                                    adStatus = it.adStatus,
                                    margin = it.margin,
                                    cryptoPrice = (getCryptoPrice - (getCryptoPrice*it.margin)),
                                    createdAt = it.createdAt,
                                    ordersMade = getMerchantData.ordersMade,
                                    ordersCompleted = getMerchantData.ordersCompleted,
                                    ordersCompletedByPercentage =getMerchantData.ordersCompletedByPercentage,
                                    paymentMethod = getMerchantData.paymentMethod,
                                    lastSeen = getMerchantData.lastSeen
                                )
                            )
                        }

                    }
                    cryptoAdResponse
                }.await()

            }catch(e:Exception){
                null
            }
        }

    }

}


