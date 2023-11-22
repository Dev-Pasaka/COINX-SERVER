package online.pasaka.infrastructure.config

object MongoDBConfig {
    val config = Config.load
    val MONGODB_URL = config.property("MONGODB_URL").getString()
    val DATABASE_NAME = config.property("DATABASE_NAME").getString()
}
