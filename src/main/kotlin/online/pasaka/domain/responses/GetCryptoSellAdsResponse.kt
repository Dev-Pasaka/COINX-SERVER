package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.cryptoAds.SellAd

@Serializable
class GetCryptoSellAdsResponse(
    val status:Boolean = false,
    val message:List<SellAd>
)