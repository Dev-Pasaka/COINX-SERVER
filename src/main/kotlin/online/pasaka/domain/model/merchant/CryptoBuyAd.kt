package online.pasaka.domain.model.merchant

import online.pasaka.domain.model.user.Phone
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class CryptoBuyAd(
    @BsonId()
    val adId: String = ObjectId().toString(),
    val merchantEmail:String,
    val merchantPhone:String,
    val merchantUsername:String,
    val ordersCompleted:Int,
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalCryptoAmount:Double,
    val minBuyAmount:Double,
    val maxBuyAmount:Double,
    var cryptoAMount:Double,
    val ordersCompletedByPercentage:Int,
    val profitMargin:Double,
    val price:Double,
)
