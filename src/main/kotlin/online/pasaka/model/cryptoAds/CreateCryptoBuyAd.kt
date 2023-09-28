package online.pasaka.model.cryptoAds

import online.pasaka.utils.Utils
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class CreateCryptoBuyAd(
    @BsonId
    val id:String = ObjectId().toString(),
    val merchantUsername:String,
    val email:String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val minLimit:Double,
    val maxLimit:Double,
    val adStatus:AdStatus,
    val margin:Double,
    val createdAt:String = Utils.currentTime()
)
