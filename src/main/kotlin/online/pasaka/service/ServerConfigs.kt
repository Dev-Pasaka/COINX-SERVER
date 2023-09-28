package online.pasaka.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.config.KafkaConfig
import online.pasaka.config.MongoDBConfig
import online.pasaka.database.DatabaseConnection
import online.pasaka.model.admin.ServerConfigs
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection

object ServerConfigs {

    private val serverConfigs = DatabaseConnection.database.getCollection<ServerConfigs>("serverConfigs")

    fun createConfigs() {

        serverConfigs.insertOne(
            ServerConfigs(
                adminEmail = "admin@coinx.co.ke",
                kafkaUrl = KafkaConfig.BOOTSTRAP_SERVER_URL,
                mongoDBUrl = ""
            )
        )

    }

    fun getServerConfigs(email: String = "admin@coinx.co.ke"): List<ServerConfigs> {

        return serverConfigs.find().filter { it.adminEmail == email }

    }
}
