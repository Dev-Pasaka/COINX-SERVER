package online.pasaka.model.merchant.wallet

import kotlinx.serialization.Serializable
import online.pasaka.utils.GetCurrentTime
import org.bson.codecs.pojo.annotations.BsonId
import java.time.temporal.TemporalAmount
import java.util.Currency
import javax.print.attribute.standard.JobOriginatingUserName

@Serializable
data class MerchantTopUps(
    @BsonId()
    val id:String,
    val fullName:String,
    val userName:String,
    val email:String,
    val currency: String = Currency.getInstance("USD").toString(),
    val amount:Double,
    val timeStamp:String
)
