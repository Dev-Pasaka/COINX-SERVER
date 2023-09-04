package online.pasaka.config

object ConfluentKafkaConfig {
    const val bootStrapServers =  "pkc-75m1o.europe-west3.gcp.confluent.cloud:9092"
    const val securityProtocol = "SASL_SSL"
    const val saslMechanism =  "PLAIN"
    const val clientdDnsLookup = "use_all_dns_ips"
    const val acks = "all"

    // Schema Registry properties
    const val schemaRegistryUrl = "https://psrc-do01d.eu-central-1.aws.confluent.cloud"
    const val  basicAuthCredentialSource =  "USER_INFO"
    const val basicAuthUserInfo = "WSL7FPX43KVLIBFJ:TH9ngcOaQDPCecTwiA/OoA6Y2fU4XnFMRyu+JjFPyLt4sVZ544am/2zCy0DRGZoJ"

}