package online.pasaka.Kafka

import online.pasaka.config.KafkaConfig
import org.apache.kafka.clients.admin.*
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import java.util.*

class KafkaAdmin(private val properties: Properties = Properties()) {

    private val adminProps = properties.apply {
        put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, KafkaConfig.BOOTSTRAP_SERVER_URL)
        put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
        put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer::class.java)
    }

    fun createTopic(
        topicName: String = "my-new-topic",
        partitions: Int = 6,
        replicationFactor: Short = 1,
        additionalConfig: MutableMap<String, String>? = null
    ): Boolean {

        return try {

            AdminClient.create(adminProps).use { adminClient ->
                val topicConfig = additionalConfig ?: emptyMap()
                val newTopic = NewTopic(topicName, partitions, replicationFactor).configs(topicConfig)
                adminClient.createTopics(listOf(newTopic)).all().get()
                true
            }

        } catch (e: Exception) {

            e.printStackTrace()
            false

        }

    }

    fun deleteTopic(topicName: String): Boolean {

        val properties = adminProps
        val adminClient = AdminClient.create(properties)
        val deleteOptions = DeleteTopicsOptions()
        deleteOptions.timeoutMs(5000)

        val deleteTopicsResult = try {
            adminClient.deleteTopics(listOf(topicName), deleteOptions)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        deleteTopicsResult?.let {
            deleteTopicsResult.all().get()
            adminClient.close()

        }

        adminClient.close()
        return deleteTopicsResult != null

    }

    fun listTopics(): MutableSet<String>? {

        val properties = adminProps
        val adminClient = AdminClient.create(properties)

        val listTopicsResult = try {

            adminClient.listTopics()

        } catch (e: Exception) {

            e.printStackTrace()
            null

        }

        // Block and get the list of topic names
        val topics: MutableSet<String>? = listTopicsResult?.names()?.get()
        topics?.let { adminClient.close() }
        adminClient.close()

        return topics


    }
    fun describeTopic(topicName: String): Triple<String, String, String> {

        val properties = adminProps
        val adminClient = AdminClient.create(properties)

        val describeTopicsResult = try {

            adminClient.describeTopics(listOf(topicName))

        } catch (e: Exception) {

            e.printStackTrace()
            null

        }

        val topicDescription = describeTopicsResult?.all()?.get()
        var topicDescriptionResult: Triple<String, String, String> = Triple("", "", "")

        topicDescription?.forEach { (topicName, description) ->

            topicDescriptionResult = Triple(
                first = "Topic: $topicName",
                second = "Partitions: ${description.partitions().size}",
                third = "Replication Factor: ${description.partitions()[0].replicas().size}"
            )

        }

        return topicDescriptionResult
    }
}

fun main() {


   // println(KafkaAdmin().describeTopic(topicName = KafkaConfig.EMAIL_NOTIFICATIONS))
//println(KafkaAdmin().describeTopic(topicName = "Test_Topic"))
// println(KafkaAdmin().createTopic(topicName = KafkaConfig.NOTIFICATIONS))
   /* repeat(10){
        println(KafkaAdmin().deleteTopic(topicName = KafkaConfig.MERCHANT_FLOAT_WITHDRAWAL))
        println(KafkaAdmin().deleteTopic(topicName = KafkaConfig.MERCHANT_FLOAT_TOP_UP))
        println(KafkaAdmin().listTopics())

    }*/
    println(KafkaAdmin().deleteTopic(topicName = KafkaConfig.EMAIL_NOTIFICATIONS))
    //println(KafkaAdmin().deleteTopic(topicName = KafkaConfig.MERCHANT_FLOAT_WITHDRAWAL))

  println(KafkaAdmin().listTopics())
}



