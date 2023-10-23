package online.pasaka.Kafka.consumers

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.Kafka.models.BuyOrderMessage
import online.pasaka.config.KafkaConfig
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.service.buyOrderService.createBuyOrder
import online.pasaka.utils.Utils
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.bson.types.ObjectId
import java.time.Duration
import java.util.*
import kotlin.system.exitProcess

suspend fun cryptoBuyOrderConsumer(
    groupId: String = "cryptoBuyOrders",
    topicName: String = KafkaConfig.CRYPTO_BUY_ORDERS
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

                val records = consumer.poll(Duration.ofMillis(100))

                for (record in records) {
                    val buyOrderMessageJson = record.value().removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    val buyOrderMessageObj = Json.decodeFromString(BuyOrderMessage.serializer(), buyOrderMessageJson)
                    val email = buyOrderMessageObj.buyersEmail
                    if (email.isNotBlank()){
                        println(buyOrderMessageObj)
                       println(
                           createBuyOrder(
                               buyOrder = BuyOrder(
                                   orderId = ObjectId().toString(),
                                   adId = buyOrderMessageObj.adId,
                                   buyersEmail = buyOrderMessageObj.buyersEmail,
                                   cryptoSymbol = buyOrderMessageObj.cryptoSymbol,
                                   cryptoName = buyOrderMessageObj.cryptoName,
                                   cryptoAmount = buyOrderMessageObj.cryptoAmount,
                                   orderStatus = OrderStatus.PENDING,
                                   expiresAt = Utils.currentTimeStampPlus(900000),
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
}



suspend fun main() {
    cryptoBuyOrderConsumer()
}