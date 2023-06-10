package online.pasaka.model.user

import kotlinx.serialization.Serializable
import org.bson.types.ObjectId
@Serializable
data class UserData(
    val id:String,
    val fullName: String,
    val username: String,
    val phoneNumber: String? = null,
    val email: String,
    val country: String
)