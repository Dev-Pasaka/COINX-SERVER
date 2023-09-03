package online.pasaka.Kafka

import kotlinx.coroutines.delay
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.*

class KafkaConsumer(
    private val bootstrapServers:String  = "localhost:9092",
    private val properties:Properties = Properties()

) {
    suspend fun consume(groupId:String = "1", topicName:String = "pasaka"):String{

        properties[ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG] = bootstrapServers
        properties[ConsumerConfig.GROUP_ID_CONFIG] = groupId
        properties[ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name
        properties[ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG] = StringDeserializer::class.java.name

        val consumer = KafkaConsumer<String, String>(properties)
        consumer.subscribe(listOf(topicName))
        while (true){
            val records = consumer.poll(java.time.Duration.ofMillis(100))
            for (record in records){
                database.add(record.toString())
                println("----------------------------------$record added -------------------------------------")
                println("\n\n\n\n\n\n\n\n\n\n\n\n\\n\\n\n\n\n\n\n\n\\n\n\n\n\n\n\n\"")
            }
            println("\n\n\n\n\n\n\n\n\n\n\n\n\\n\\n\n\n\n\n\n\n\\n\n\n\n\n\n\n\"")
            println("------------------------------$database---------------------------")

        }


    }
}

val database = mutableListOf<String>()
