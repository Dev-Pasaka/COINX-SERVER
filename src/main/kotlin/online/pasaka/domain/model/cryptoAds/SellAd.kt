package online.pasaka.domain.model.cryptoAds

import kotlinx.serialization.Serializable
import online.pasaka.domain.utils.Utils
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
@Serializable
data class SellAd(
    @BsonId
    val id:String = ObjectId().toString(),
    val merchantUsername:String,
    val email:String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val margin:Double,
    val minLimit:Double,
    val maxLimit:Double,
    val adStatus: AdStatus = AdStatus.OPEN,
    val createdAt:String = Utils.currentTimeStamp()
)
