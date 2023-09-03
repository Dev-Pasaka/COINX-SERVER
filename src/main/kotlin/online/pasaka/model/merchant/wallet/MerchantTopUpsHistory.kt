package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.Currency

@Serializable
data class MerchantTopUpsHistory(
    @BsonId()
    val id:String = ObjectId().toString(),
    val fullName:String,
    val userName:String,
    val email:String,
    val currency: String = Currency.getInstance("USD").toString(),
    val amount:Double,
    val totalBalance:Double,
    val timeStamp:String
)
