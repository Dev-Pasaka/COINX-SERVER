package online.pasaka.model.merchant.wallet

import org.bson.codecs.pojo.annotations.BsonId
import java.util.*

data class MerchantsWithdrawals(
    @BsonId()
    val id:String,
    val fullName:String,
    val userName:String,
    val email:String,
    val currency: String = Currency.getInstance("USD").toString(),
    val amount:Double,
    val timeStamp:String
)
