package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.dto.cryptoAds.GetSellAdDto
import online.pasaka.domain.model.cryptoAds.SellAd
@Serializable
data class GetSellAdsFullDataResponse(
    val status:Boolean = false,
    val message:List<GetSellAdDto>
)