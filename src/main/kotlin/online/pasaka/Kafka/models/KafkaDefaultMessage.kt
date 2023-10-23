package online.pasaka.Kafka.models

import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class KafkaDefaultMessage(
    val email:String,
    val message: String
)
