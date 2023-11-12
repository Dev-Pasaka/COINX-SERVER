package online.pasaka.model.merchant

import online.pasaka.model.user.PaymentMethod
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.util.Objects

data class Merchant(
    @BsonId()
    val id:String = ObjectId().toString(),
    val fullName: String,
    val username: String,
    val phoneNumber:String,
    val email: String,
    val password: String,
    val ordersMade:Long = 0,
    val ordersCompleted:Long = 0,
    val ordersCompletedByPercentage:Double = 0.0,
    var createdAt: String = "",
    val country: String = "Kenya",
    var kycVerification:Boolean = false,
    var paymentMethod: PaymentMethod? = null,
    val lastSeen:String = ""
)
