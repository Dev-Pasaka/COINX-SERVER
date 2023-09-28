package online.pasaka.Kafka

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json.Default.decodeFromString
import online.pasaka.config.KafkaConfig
import online.pasaka.model.merchant.wallet.MerchantFloatTopUpMessage
import online.pasaka.model.merchant.wallet.MerchantFloatWithdrawalMessage
import online.pasaka.service.MerchantServices
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

class KafkaConsumers {

    private val processedOffsets: MutableSet<Pair<String, Int>> = mutableSetOf()

    suspend fun merchantTopUpConsumer(
        groupId: String = "ktor-consumer",
        topicName: String = KafkaConfig.MERCHANT_FLOAT_TOP_UP
    ) {

        coroutineScope {

            launch {

                val consumerProps = Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVER_URL)
                    put("key.deserializer", StringDeserializer::class.java)
                    put("value.deserializer", StringDeserializer::class.java)
                    put("group.id", groupId)
                    put("session.timeout.ms", 45000)
                }

                val consumer = KafkaConsumer<Nothing, String>(consumerProps)
                consumer.subscribe(listOf(topicName))

                while (true) {

                    val records = consumer.poll(java.time.Duration.ofMillis(100))

                    for (record in records) {

                        val merchantTopUpJson = record.value()
                        val merchantTopUpObj =
                            decodeFromString(MerchantFloatTopUpMessage.serializer(), merchantTopUpJson)
                        val email = merchantTopUpObj.email

                        if (email.isBlank()) {

                            continue

                        } else {
                            println("--------------------------${merchantTopUpObj}-------------------------")
                            if (merchantTopUpObj.crypto.toDoubleOrNull() != null) {
                                MerchantServices.merchantTopUpFloat(
                                    email = email,
                                    crypto = merchantTopUpObj.crypto.toDouble()
                                )
                            }

                        }


                    }

                }
            }
        }
    }

    private suspend fun merchantWithdrawalConsumer(
        groupId: String = "ktor-consumer",
        topicName: String = KafkaConfig.MERCHANT_FLOAT_WITHDRAWAL
    ) {

        coroutineScope {

            launch {

                val consumerProps = Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVER_URL)
                    put("key.deserializer", StringDeserializer::class.java)
                    put("value.deserializer", StringDeserializer::class.java)
                    put("group.id", groupId)
                    put("session.timeout.ms", 45000)
                }

                val consumer = KafkaConsumer<String, String>(consumerProps)
                consumer.subscribe(listOf(topicName))

                while (true) {
                    val records = consumer.poll(java.time.Duration.ofMillis(100))

                    for (record in records) {
                        val merchantTopUpJson = record.value()
                        val merchantTopUpObj =
                            decodeFromString(MerchantFloatWithdrawalMessage.serializer(), merchantTopUpJson)
                        val email = merchantTopUpObj.email
                        if (email.isBlank()) {
                            continue
                        } else {
                            println("--------------------------${merchantTopUpObj}-------------------------")
                            if (merchantTopUpObj.amount.toDoubleOrNull() != null) {
                                MerchantServices.merchantWithdrawalFloat(
                                    email = email,
                                    crypto = merchantTopUpObj.amount.toDouble()
                                )
                            }

                        }

                    }

                }
            }
        }
    }

    suspend fun startConsumers() {

        coroutineScope {

            launch {

                merchantTopUpConsumer()

            }
            launch {

                merchantWithdrawalConsumer()

            }
        }
    }

}


suspend fun main() {
    coroutineScope {
        repeat(1000) {
            launch {
                KafkaProducer().merchantTopUpProducer(
                    email = "dev.pasaka@gmail.com",
                    topic = KafkaConfig.MERCHANT_FLOAT_TOP_UP,
                    message = "10"
                )
            }
        }
        launch {
            KafkaConsumers().merchantTopUpConsumer()
        }
    }
}

