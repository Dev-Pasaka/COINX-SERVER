package online.pasaka.domain.dto.merchant

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.user.PaymentMethod
import org.bson.types.ObjectId
@Serializable
data class MerchantDataDto(
    val username: String = "",
    val phoneNumber:String = "",
    val email: String = "",
    val ordersMade:Long = 0,
    val ordersCompleted:Long = 0,
    val ordersCompletedByPercentage:Double = 0.0,
    var paymentMethod: PaymentMethod? = null,
    val lastSeen:String = ""
)
