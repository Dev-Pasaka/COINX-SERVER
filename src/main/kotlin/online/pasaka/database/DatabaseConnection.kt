package online.pasaka.database

import com.mongodb.client.MongoDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.config.MongoDBConfig
import online.pasaka.service.ServerConfigs
import org.litote.kmongo.KMongo

object DatabaseConnection {


        private val client = KMongo.createClient(MongoDBConfig.MONGODB_URL)
        val database: MongoDatabase = client.getDatabase(MongoDBConfig.Database_Name)
        fun closeConnection(){ client.close() }



}