package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.dto.cryptoAds.GetBuyAdDto
import online.pasaka.domain.model.cryptoAds.SellAd
@Serializable
data class GetBuyAdsFullDataResponse(
    val status:Boolean = false,
    val message:List<GetBuyAdDto>
)
