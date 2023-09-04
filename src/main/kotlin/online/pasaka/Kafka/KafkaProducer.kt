package online.pasaka.Kafka


import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import online.pasaka.config.KafkaConfig
import online.pasaka.model.merchant.wallet.MerchantFloatTopUp
import online.pasaka.model.merchant.wallet.MerchantFloatTopUpMessage
import online.pasaka.model.merchant.wallet.MerchantFloatWithdrawalMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class KafkaProducer{
    /*init {
        // Set the JAAS configuration
        System.setProperty(
            "java.security.auth.login.config",
            "/home/paskado/Downloads/coinx-server/src/main/resources/jass.conf"
        )
    }*/

    suspend fun merchantTopUpProducer(
        email:String,
        topic: String = "pasaka",
        properties:Properties = Properties(),
        message: MerchantFloatTopUp
    )  = coroutineScope{
        launch {


            val producerProps = properties.apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAPSERVER)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            }

            val producer = KafkaProducer<Nothing, String>(producerProps)
            val merchantTopUp = MerchantFloatTopUpMessage(
                email = email,
                amount = message.amount,
                currency = message.currency
            )

            val json = Json.encodeToString(merchantTopUp)
            val record = ProducerRecord(topic, null, json)

            val result = producer.send(record)
            println(result)
            producer.close()
        }


    }

    suspend fun merchantWithdrawalProducer(
        email:String,
        topic: String = "pasaka",
        properties:Properties = Properties(),
        message: MerchantFloatTopUp
    )  = coroutineScope{
        launch {


            val producerProps = properties.apply {
                put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAPSERVER)
                put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
                put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
            }

            val producer = KafkaProducer<Nothing, String>(producerProps)
            val merchantTopUp = MerchantFloatWithdrawalMessage(
                email = email,
                amount = message.amount,
                currency = message.currency
            )

            val json = Json.encodeToString(merchantTopUp)
            val record = ProducerRecord(topic, null, json)

            val result = producer.send(record)
            println(result)
            producer.close()
        }


    }
}

suspend fun main() {
     repeat(1000){
        KafkaProducer().merchantTopUpProducer(
            email = "dev.pasaka@gmail.com",
            topic = "MerchantFloatTopUp",
            message = MerchantFloatTopUp( amount = 10.2, currency = "USD")
        )
    }

}




