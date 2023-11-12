package online.pasaka.Kafka.consumers

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import online.pasaka.config.KafkaConfig
import online.pasaka.model.merchant.wallet.MerchantFloatTopUpMessage
import online.pasaka.service.merchantServices.MerchantServices
import online.pasaka.threads.Threads
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors

@OptIn(DelicateCoroutinesApi::class)
suspend fun merchantTopUpConsumer(
    groupId: String = "ktor-consumer",
    topicName: String = KafkaConfig.MERCHANT_FLOAT_TOP_UP
) {
    val kafkaUrl = KafkaConfig.BOOTSTRAP_SERVER_URL
    val username = KafkaConfig.KAFKA_USERNAME
    val password = KafkaConfig.KAFKA_PASSWORD

    val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.CONSUMERS)
    }.asCoroutineDispatcher()

    val coroutineScope = CoroutineScope(customDispatcher)
    coroutineScope.launch {
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

                val records = consumer.poll(Duration.ofMillis(100))

                for (record in records) {

                    val merchantTopUpJson = record.value()
                    val merchantTopUpObj =
                        Json.decodeFromString(MerchantFloatTopUpMessage.serializer(), merchantTopUpJson)
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