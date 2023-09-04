package online.pasaka.Kafka

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json.Default.decodeFromString
import online.pasaka.config.KafkaConfig
import online.pasaka.model.merchant.wallet.MerchantFloatTopUp
import online.pasaka.model.merchant.wallet.MerchantFloatTopUpMessage
import online.pasaka.model.merchant.wallet.MerchantFloatWithdrawalMessage
import online.pasaka.service.MerchantServices
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*
class KafkaConsumers {

    private val processedOffsets: MutableSet<Pair<String, Int>> = mutableSetOf() // Keep track of processed offsets

    suspend fun merchantTopUpConsumer(groupId: String = "ktor-consumer", topicName: String = "MerchantFloatTopUp") {
        coroutineScope {
            launch {
                val consumerProps = Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAPSERVER)
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
                        val merchantTopUpObj = decodeFromString(MerchantFloatTopUpMessage.serializer(), merchantTopUpJson)
                        val email = merchantTopUpObj.email
                        if (email.isNullOrBlank()){
                            continue
                        }else{
                            MerchantServices.merchantTopUpFloat(
                                email = email,
                                amount = merchantTopUpObj.amount,
                                currency = merchantTopUpObj.currency
                            )


                            println("--Partition: ${record.partition()} Offset: ${record.offset()} Topic: ${record.topic()}")
                            println(record.value())
                        }




                    }
                    //delay(10000)
                }
            }
        }
    }

    suspend fun merchantWithdrawalConsumer(groupId: String = "ktor-consumer", topicName: String = "MerchantFloatWithdrawal") {
        coroutineScope {
            launch {
                val consumerProps = Properties().apply {
                    put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAPSERVER)
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
                        val merchantTopUpObj = decodeFromString(MerchantFloatWithdrawalMessage.serializer(), merchantTopUpJson)
                        val email = merchantTopUpObj.email
                        if (email.isNullOrBlank()){
                            continue
                        }else{
                            MerchantServices.merchantWithdrawalFloat(
                                email = email,
                                amount = merchantTopUpObj.amount,
                                currency = merchantTopUpObj.currency
                            )
                            println("--Partition: ${record.partition()} Offset: ${record.offset()} Topic: ${record.topic()}")
                            println(record.value())
                        }




                    }
                    //delay(10000)
                }
            }
        }
    }

    suspend fun startConsumers(){
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

val database = mutableListOf<String>()


suspend fun main(){
    KafkaConsumers().merchantTopUpConsumer(
        topicName = "MerchantFloatTopUp"
    )
}
