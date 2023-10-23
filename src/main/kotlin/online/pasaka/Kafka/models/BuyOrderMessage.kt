package online.pasaka.Kafka.models

import kotlinx.serialization.Serializable
@Serializable
data class BuyOrderMessage(
    val adId: String,
    val buyersEmail: String,
    val cryptoName: String,
    val cryptoSymbol: String,
    val cryptoAmount: Double,

)
