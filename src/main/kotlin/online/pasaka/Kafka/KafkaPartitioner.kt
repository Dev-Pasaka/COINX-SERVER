package online.pasaka.Kafka

import com.google.common.hash.Hashing
import io.confluent.common.utils.Utils
import org.apache.kafka.clients.producer.Partitioner
import org.apache.kafka.common.Cluster
import kotlin.math.abs
import kotlin.random.Random

class KafkaPartitioner : Partitioner {
    private var currentPartition = 0

    override fun configure(configs: MutableMap<String, *>?) {}

    override fun close() {}

    override fun partition(
        topic: String?,
        key: Any?,
        keyBytes: ByteArray?,
        value: Any?,
        valueBytes: ByteArray?,
        cluster: Cluster?
    ): Int {

        val availablePartitions = cluster?.partitionsForTopic("MerchantFloatTopUp")?.size

        return synchronized(this) {

            val partition = currentPartition
            currentPartition = (currentPartition + 1) % availablePartitions!!
            partition

        }
    }
}