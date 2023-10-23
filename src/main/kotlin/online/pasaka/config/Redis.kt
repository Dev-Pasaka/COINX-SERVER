package online.pasaka.config

object Redis {
    val config = Config.load
    val REDIS_CLOUD_URL = config.property("REDIS_CLOUD_URL").getString()

    /**Redis Key Value Pairs*/
    const val FOREIGN_EXCHANGE_IN_KES_KEY = "KES"
}