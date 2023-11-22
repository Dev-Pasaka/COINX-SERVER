package online.pasaka.infrastructure.config

import online.pasaka.infrastructure.database.DatabaseConnection
import online.pasaka.domain.model.admin.ServerConfigs
import org.litote.kmongo.getCollection
import javax.management.Notification

object KafkaConfig {
    val config = Config.load
   val BOOTSTRAP_SERVER_URL = config.property("KAFKA_URL").getString()
    val KAFKA_USERNAME= config.property("KAFKA_USERNAME").getString()
    val KAFKA_PASSWORD = config.property("KAFKA_PASSWORD").getString()
    const val MERCHANT_FLOAT_TOP_UP = "MerchantFloatTopUp"
    const val MERCHANT_FLOAT_WITHDRAWAL = "MerchantFloatWithdrawal"
    const val CRYPTO_BUY_ORDERS = "CryptoBuyOrders"
    const val EMAIL_NOTIFICATIONS = "EmailNotifications"
    const val CRYPTO_SELL_ORDERS = "CryptoSellOrders"

}



