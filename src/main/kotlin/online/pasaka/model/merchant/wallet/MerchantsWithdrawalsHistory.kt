package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

@Serializable
data class MerchantsWithdrawalsHistory(
    @BsonId()
    val id:String = ObjectId().toString(),
    val fullName:String,
    val userName:String,
    val email:String,
    val crypto: String = "USDT",
    val usdtAmount:Double,
    val timeStamp:String
)
