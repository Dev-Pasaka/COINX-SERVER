package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.cryptoAds.BuyAd

@Serializable
data class GetCryptoBuyAdsResponse(
    val status:Boolean = false,
    val message:List<BuyAd>
)