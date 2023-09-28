package online.pasaka.model.admin

import kotlinx.serialization.Serializable
import online.pasaka.config.KafkaConfig
import online.pasaka.config.MongoDBConfig

@Serializable
data class ServerConfigs(
    val adminEmail:String,
    val kafkaUrl:String = KafkaConfig.BOOTSTRAP_SERVER_URL,
    val mongoDBUrl:String = ""
)
