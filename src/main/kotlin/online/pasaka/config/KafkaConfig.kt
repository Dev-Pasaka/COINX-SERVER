package online.pasaka.config

import online.pasaka.database.DatabaseConnection
import online.pasaka.model.admin.ServerConfigs
import org.litote.kmongo.getCollection

object KafkaConfig {
    val config = Config.load
    val BOOTSTRAP_SERVER_URL = config.property("KAFKA_URL").getString()
    const val MERCHANT_FLOAT_TOP_UP = "MerchantFloatTopUp"
    const val MERCHANT_FLOAT_WITHDRAWAL = "MerchantFloatWithdrawal"

}



