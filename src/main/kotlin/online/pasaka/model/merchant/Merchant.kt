package online.pasaka.model.user.merchant

import online.pasaka.model.user.PaymentMethod
import org.bson.codecs.pojo.annotations.BsonId

data class Merchant(
    @BsonId()
    val id:String,
    val fullName: String,
    val username: String,
    val phoneNumber:String,
    val email: String,
    val password: String,
    var createdAt: String = "",
    val country: String = "Kenya",
    var kycVerification:Boolean = false,
    val merchant:Boolean = false,
    var paymentMethod: PaymentMethod? = null
)
