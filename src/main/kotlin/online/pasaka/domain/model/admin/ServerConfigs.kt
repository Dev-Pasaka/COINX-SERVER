package online.pasaka.domain.model.admin

import kotlinx.serialization.Serializable
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.config.MongoDBConfig

@Serializable
data class ServerConfigs(
    val adminEmail:String,
    val kafkaUrl:String = KafkaConfig.BOOTSTRAP_SERVER_URL,
    val mongoDBUrl:String = ""
)
