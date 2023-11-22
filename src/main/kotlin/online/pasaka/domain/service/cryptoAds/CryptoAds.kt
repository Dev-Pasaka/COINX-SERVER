package online.pasaka.domain.service.cryptoAds

import online.pasaka.domain.dto.cryptoAds.GetSellAdDto
import online.pasaka.domain.repository.database.cryptoAds.CryptoAdsRepositoryImpl
import online.pasaka.domain.responses.*

object CryptoAds {
    suspend fun getCryptoBuyAds(): GetCryptoBuyAdsResponse {
        val result = CryptoAdsRepositoryImpl().getBuyAds()
        return when (result.isNotEmpty()) {
            true -> GetCryptoBuyAdsResponse(status = true, message = result)
            false -> GetCryptoBuyAdsResponse(status = false, message = result)
        }
    }

    suspend fun getCryptoSellAds(): GetCryptoSellAdsResponse {
        val result = CryptoAdsRepositoryImpl().getSellAds()
        return when (result.isNotEmpty()) {
            true -> GetCryptoSellAdsResponse(status = true, message = result)
            false -> GetCryptoSellAdsResponse(status = false, message = result)
        }
    }

    suspend fun getCryptoSellAdsFullData(): GetSellAdsFullDataResponse {
        val result = CryptoAdsRepositoryImpl().getSellAdsData()
        return when (result?.isNotEmpty()) {
            true -> GetSellAdsFullDataResponse(status = true, message = result)
            false -> GetSellAdsFullDataResponse(status = false, message = result)
            else -> GetSellAdsFullDataResponse(status = false, message = emptyList())
        }
    }

    suspend fun getCryptoBuyAdsFullData(): GetBuyAdsFullDataResponse {
        val result = CryptoAdsRepositoryImpl().getBuyAdsData()
        return when (result?.isNotEmpty()) {
            true -> GetBuyAdsFullDataResponse(status = true, message = result)
            false -> GetBuyAdsFullDataResponse(status = false, message = result)
            else -> GetBuyAdsFullDataResponse(status = false, message = emptyList())
        }
    }
}