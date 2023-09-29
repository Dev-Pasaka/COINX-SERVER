package online.pasaka.database

import com.mongodb.client.MongoDatabase
import online.pasaka.config.MongoDBConfig
import org.litote.kmongo.KMongo

object DatabaseConnection {


        private val client = KMongo.createClient(MongoDBConfig.MONGODB_URL)
        val database: MongoDatabase = client.getDatabase(MongoDBConfig.DATABASE_NAME)
        fun closeConnection(){ client.close() }



}