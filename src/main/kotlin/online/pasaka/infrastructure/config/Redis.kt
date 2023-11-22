package online.pasaka.infrastructure.config

import com.google.gson.Gson
import online.pasaka.domain.repository.remote.cryptodata.GetAllCryptoPrices
import redis.clients.jedis.Jedis

object Redis {
    val config = Config.load
    val REDIS_CLOUD_URL = config.property("REDIS_CLOUD_URL").getString()
    val REDIS_PORT = config.property("REDIS_CLOUD_PORT").getString()

    /**Redis Key Value Pairs*/
    const val FOREIGN_EXCHANGE_IN_KES_KEY = "KES"
    const val CRYPTO_PRICES = "CRYPTO"
}

