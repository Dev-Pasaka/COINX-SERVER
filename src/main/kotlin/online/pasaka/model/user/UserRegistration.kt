package online.pasaka.model.user

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
@Serializable
data class UserRegistration(
    @BsonId()
    val id:String = ObjectId().toString(),
    val fullName: String,
    val username: String,
    val phoneNumber:String,
    val email: String,
    val password: String,
)
