package online.pasaka.model.user

import kotlinx.serialization.Serializable
import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Serializable
data class User(

    @BsonId()
    val id:String = ObjectId().toString(),
    val fullName: String,
    val username: String,
    val phoneNumber:String,
    val email: String,
    val password: String,
    var createdAt: String = "",
    val country: String = "Kenya",
){
}
