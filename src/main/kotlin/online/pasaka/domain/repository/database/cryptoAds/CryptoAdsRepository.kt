package online.pasaka.domain.repository.database.cryptoAds


import online.pasaka.domain.dto.cryptoAds.GetBuyAdDto
import online.pasaka.domain.dto.cryptoAds.GetSellAdDto
import online.pasaka.domain.model.cryptoAds.BuyAd
import online.pasaka.domain.model.cryptoAds.SellAd

interface CryptoAdsRepository {
    suspend fun getBuyAds(): List<BuyAd>
    suspend fun getSellAds(): List<SellAd>

    suspend fun getBuyAdsData(): List<GetBuyAdDto>?
    suspend fun getSellAdsData(): List<GetSellAdDto>?

}