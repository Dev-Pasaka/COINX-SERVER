package online.pasaka.Kafka.consumers

import com.google.gson.Gson
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.Kafka.models.BuyOrderConfirmationNotificationMessage
import online.pasaka.Kafka.models.BuyOrderMessage
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.config.KafkaConfig
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.service.buyOrderService.createBuyOrder
import online.pasaka.service.mailService.sendBuyOrderConfirmationEmail
import online.pasaka.utils.Utils
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.bson.types.ObjectId
import java.time.Duration
import java.util.*
import kotlin.system.exitProcess

suspend fun emailNotificationConsumer(
    groupId: String = "emailNotificationConsumers",
    topicName: String = KafkaConfig.EMAIL_NOTIFICATIONS
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
                    println(record.value())
                    val message = record.value().removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    println(message)
                    val gson = Gson()
                    val result = gson.fromJson(message, Notification::class.java)
                    when (result.notificationType) {
                        NotificationType.ORDER_HAS_BEEN_PLACED ->{
                            try {
                                val message = result.notificationMessage as? Map<String, Any>
                                sendBuyOrderConfirmationEmail(
                                    orderID =  message?.get("orderId")?.toString() ?: "",
                                    recipientName = message?.get("recipientName")?.toString() ?:"",
                                    recipientEmail = message?.get("recipientEmail")?.toString() ?:"",
                                    cryptoName = message?.get("cryptoName")?.toString() ?: "",
                                    cryptoSymbol =  message?.get("cryptoSymbol")?.toString() ?: "",
                                    cryptoAmount = message?.get("cryptoAmount")?.toString()?.toDoubleOrNull() ?: 0.0,
                                    amountToReceive = message?.get("amountInKes")?.toString()?.toDoubleOrNull() ?: 0.0,

                                )
                            }catch (e:Exception){
                                e.printStackTrace()
                            }

                        }
                        NotificationType.EXPIRED ->{

                        }NotificationType.CANCELLED ->{

                        }
                        NotificationType.COMPLETED ->{

                        }
                        else -> {}
                    }
                }


            }

        }
    }
}

suspend fun main() {
    emailNotificationConsumer()
}