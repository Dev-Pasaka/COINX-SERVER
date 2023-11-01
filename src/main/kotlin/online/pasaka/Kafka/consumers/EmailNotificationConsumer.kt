package online.pasaka.Kafka.consumers

import com.google.gson.Gson
import kotlinx.coroutines.*
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.config.KafkaConfig
import online.pasaka.service.mailService.sendEmail
import online.pasaka.threads.Threads
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors

@OptIn(DelicateCoroutinesApi::class)
suspend fun emailNotificationConsumer(
    groupId: String = "emailNotificationConsumers",
    topicName: String = KafkaConfig.EMAIL_NOTIFICATIONS
) {
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
                    println(record.value())
                    val message = record.value().removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    println(message)
                    val gson = Gson()
                    val notificationMessage = gson.fromJson(message, Notification::class.java)
                    println(notificationMessage)
                    when (notificationMessage.notificationType) {
                        NotificationType.ORDER_HAS_BEEN_PLACED ->{
                            try {
                                val result = notificationMessage.notificationMessage as? Map<String, Any> ?: emptyMap()
                                launch(Dispatchers.IO) {
                                    sendEmail(
                                        title = result["title"].toString(),
                                        orderID = result["orderId"]?.toString() ?: "",
                                        recipientName = result["recipientName"]?.toString() ?: "",
                                        recipientEmail = result["recipientEmail"]?.toString() ?: "",
                                        cryptoName = result["cryptoName"]?.toString() ?: "",
                                        cryptoSymbol = result["cryptoSymbol"]?.toString() ?: "",
                                        cryptoAmount = result["cryptoAmount"]?.toString()?.toDoubleOrNull() ?: 0.0,
                                        amountToReceive = result["amountInKes"]?.toString()?.toDoubleOrNull() ?: 0.0,

                                        )
                                }
                            }catch (e:Exception){
                                e.printStackTrace()
                            }

                        }
                        NotificationType.EXPIRED ->{
                            val result = notificationMessage.notificationMessage as? Map<String, Any> ?: emptyMap()
                            launch(Dispatchers.IO) {
                                sendEmail(
                                    title = result["title"].toString(),
                                    orderID = result["orderId"]?.toString() ?: "",
                                    recipientName = result["recipientName"]?.toString() ?: "",
                                    recipientEmail = result["recipientEmail"]?.toString() ?: "",
                                    cryptoName = result["cryptoName"]?.toString() ?: "",
                                    cryptoSymbol = result["cryptoSymbol"]?.toString() ?: "",
                                    cryptoAmount = result["cryptoAmount"]?.toString()?.toDoubleOrNull() ?: 0.0,
                                    amountToReceive = result["amountInKes"]?.toString()?.toDoubleOrNull() ?: 0.0,

                                    )
                            }

                        }
                        NotificationType.CANCELLED ->{

                        println(notificationMessage)
                        val result = notificationMessage.notificationMessage as? Map<String, Any> ?: emptyMap()
                            launch(Dispatchers.IO) {
                                sendEmail(
                                    title = result["title"].toString(),
                                    recipientName = result["recipientName"].toString(),
                                    recipientEmail = result["recipientEmail"].toString(),
                                    orderID = result["orderId"].toString(),
                                    cryptoAmount = result["cryptoAmount"].toString().toDoubleOrNull() ?: 0.0,
                                    cryptoSymbol = result["cryptoSymbol"].toString(),
                                    cryptoName = result["cryptoName"].toString(),
                                    amountToReceive = result["amountInKes"].toString().toDoubleOrNull() ?: 0.0,
                                )
                            }

                        }
                        NotificationType.COMPLETED ->{
                            val result = notificationMessage.notificationMessage as? Map<String, Any> ?: emptyMap()
                            launch(Dispatchers.IO) {
                                sendEmail(
                                    title = result["title"].toString(),
                                    recipientName = result["recipientName"].toString(),
                                    recipientEmail = result["recipientEmail"].toString(),
                                    orderID = result["orderId"].toString(),
                                    cryptoAmount = result["cryptoAmount"].toString().toDoubleOrNull() ?: 0.0,
                                    cryptoSymbol = result["cryptoSymbol"].toString(),
                                    cryptoName = result["cryptoName"].toString(),
                                    amountToReceive = result["amountInKes"].toString().toDoubleOrNull() ?: 0.0,
                                )
                            }

                        }
                        NotificationType.BUYER_HAS_TRANSFERRED_FUNDS ->{

                            println(notificationMessage)
                            val result = notificationMessage.notificationMessage as? Map<String, Any> ?: emptyMap()
                            launch(Dispatchers.IO) {
                                sendEmail(
                                    title = result["title"].toString(),
                                    recipientName = result["recipientName"].toString(),
                                    recipientEmail = result["recipientEmail"].toString(),
                                    orderID = result["orderId"].toString(),
                                    cryptoAmount = result["cryptoAmount"].toString().toDoubleOrNull() ?: 0.0,
                                    cryptoSymbol = result["cryptoSymbol"].toString(),
                                    cryptoName = result["cryptoName"].toString(),
                                    amountToReceive = result["amountInKes"].toString().toDoubleOrNull() ?: 0.0,
                                )
                            }
                            //exitProcess(status = 2)
                        }

                        else -> {}
                    }
                }


            }


    }
}

suspend fun main() {
    emailNotificationConsumer()
}