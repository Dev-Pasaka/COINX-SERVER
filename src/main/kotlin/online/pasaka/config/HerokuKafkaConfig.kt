import com.github.jkutner.EnvKeyStore
import online.pasaka.config.HerokuKafkaConfigConsts
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import java.net.URI
import java.net.URISyntaxException
import java.util.*

class HerokuKafkaConfig(private val topic: String, private val consumerGroup: String) {

    fun getProperties(): Properties {
        return buildDefaults()
    }

    private fun buildDefaults(): Properties {
        val properties = Properties()
        val hostPorts = mutableListOf<String>()

        for (url in HerokuKafkaConfigConsts.KAFKA_URL.split(",")) {
            try {
                val uri = URI(url)
                hostPorts.add("${uri.host}:${uri.port}")

                when (uri.scheme) {
                    "kafka" -> {
                        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "PLAINTEXT"
                    }
                    "kafka+ssl" -> {
                        properties[CommonClientConfigs.SECURITY_PROTOCOL_CONFIG] = "SSL"
                        properties["ssl.endpoint.identification.algorithm"] = ""
                        val kafkaTrustedCert = System.getenv("KAFKA_TRUSTED_CERT")
                        println(kafkaTrustedCert)

                            val envTrustStore = EnvKeyStore.createWithRandomPassword(kafkaTrustedCert)
                            val envKeyStore = EnvKeyStore.createWithRandomPassword(HerokuKafkaConfigConsts.KAFKA_CLIENT_CERT_KEY, HerokuKafkaConfigConsts.KAFKA_CLIENT_CERT)

                            val trustStore = envTrustStore.storeTemp()
                            val keyStore = envKeyStore.storeTemp()

                            properties[SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG] = envTrustStore.type()
                            properties[SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG] = trustStore.absolutePath
                            properties[SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG] = envTrustStore.password()
                            properties[SslConfigs.SSL_KEYSTORE_TYPE_CONFIG] = envKeyStore.type()
                            properties[SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG] = keyStore.absolutePath
                            properties[SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG] = envKeyStore.password()

                    }
                    else -> throw IllegalArgumentException("unknown scheme: ${uri.scheme}")
                }
            } catch (e: URISyntaxException) {
                throw RuntimeException(e)
            }
        }

        properties[CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG] = hostPorts.joinToString(",")
        return properties
    }
}
