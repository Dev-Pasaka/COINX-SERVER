package online.pasaka.Kafka


import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class KafkaProducer(private val bootstrapServers:String = "localhost:9092") {
    fun produce(groupId:String,topic:String = "pasaka", key:String, message:Any){
        val producerProps = Properties().apply {
            put("bootstrap.servers", bootstrapServers)
            put("key.serializer", StringSerializer::class.java)
            put("value.serializer", StringSerializer::class.java)
            put(ConsumerConfig.GROUP_ID_CONFIG,groupId)

        }
        val producer = KafkaProducer<String, String>(producerProps)
        val objectMapper = ObjectMapper()

        val value = objectMapper.writeValueAsString(message.toString())
        val record = ProducerRecord(topic, key, value)

        producer.send(record)
        producer.close()
    }
}