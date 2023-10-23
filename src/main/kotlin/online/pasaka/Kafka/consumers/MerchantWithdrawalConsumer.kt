package online.pasaka.Kafka.consumers

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.config.KafkaConfig
import online.pasaka.model.merchant.wallet.MerchantFloatWithdrawalMessage
import online.pasaka.service.MerchantServices
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*

suspend fun merchantWithdrawalConsumer(
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
                val records = consumer.poll(Duration.ofMillis(100))

                for (record in records) {
                    val merchantTopUpJson = record.value()
                    val merchantTopUpObj =
                        Json.decodeFromString(MerchantFloatWithdrawalMessage.serializer(), merchantTopUpJson)
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