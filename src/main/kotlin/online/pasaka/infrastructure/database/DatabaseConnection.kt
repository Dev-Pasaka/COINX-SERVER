package online.pasaka.infrastructure.database

import com.mongodb.client.MongoDatabase
import online.pasaka.infrastructure.config.MongoDBConfig
import org.litote.kmongo.KMongo

object DatabaseConnection {
        /** MongoDd Client Instance*/
        private val client = KMongo.createClient(MongoDBConfig.MONGODB_URL)
        /** Database Instantiation of client*/
        val database: MongoDatabase = client.getDatabase(MongoDBConfig.DATABASE_NAME)
}