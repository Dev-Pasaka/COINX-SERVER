package online.pasaka.Kafka.consumers

import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.messages.SellOrderMessage
import online.pasaka.domain.model.order.OrderStatus
import online.pasaka.domain.model.order.SellOrder
import online.pasaka.domain.service.orders.sellOrderService.createSellOrder
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.threads.Threads
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.bson.types.ObjectId
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors

suspend fun sellOrderConsumer(
    groupId: String = "cryptoSellOrders",
    topicName: String = KafkaConfig.CRYPTO_SELL_ORDERS
) {
    val gson = Gson()
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
                val sellOrderMessageJson = record.value().removePrefix("\"")
                    .removeSuffix("\"")
                    .replace("\\", "")

                val sellOrderMessageObj = gson.fromJson(sellOrderMessageJson, SellOrderMessage::class.java )
                val email = sellOrderMessageObj.sellersEmail
                if (email.isNotBlank()){
                    println(sellOrderMessageObj)
                    println(
                        createSellOrder(
                            sellOrder = SellOrder(
                                orderId = ObjectId().toString(),
                                adId = sellOrderMessageObj.adId,
                                sellerEmail = sellOrderMessageObj.sellersEmail,
                                cryptoSymbol = sellOrderMessageObj.cryptoSymbol,
                                cryptoName = sellOrderMessageObj.cryptoName,
                                cryptoAmount = sellOrderMessageObj.cryptoAmount,
                                orderStatus = OrderStatus.PENDING,
                                expiresAt = System.currentTimeMillis() + (60000 * 15).toLong(),
                                amountInKes = 0.0
                            )
                        )
                    )
                }else{

                }

            }


        }

    }

}

