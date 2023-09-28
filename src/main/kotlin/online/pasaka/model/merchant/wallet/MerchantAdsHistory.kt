package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
@Serializable
data class MerchantAdsHistory(
    @BsonId()
    val id:String = ObjectId().toString(),
    val fullName:String,
    val userName:String,
    val email:String,
    val cryptoSymbol: String,
    val cryptoName: String,
    val cryptoAmount:Double,
    val adType:String,
    val createdAt:String
)
