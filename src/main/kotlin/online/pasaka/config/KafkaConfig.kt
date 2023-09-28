package online.pasaka.config

import online.pasaka.database.DatabaseConnection
import online.pasaka.model.admin.ServerConfigs
import org.litote.kmongo.getCollection

object KafkaConfig {

    const val BOOTSTRAP_SERVER_URL = "ec2-3-71-158-169.eu-central-1.compute.amazonaws.com:9092"
    const val MERCHANT_FLOAT_TOP_UP = "MerchantFloatTopUp"
    const val MERCHANT_FLOAT_WITHDRAWAL = "MerchantFloatWithdrawal"

}



