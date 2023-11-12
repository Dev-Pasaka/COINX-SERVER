package online.pasaka.Kafka.models.messages

data class SellOrderMessage(
    val adId: String,
    val sellersEmail: String,
    val cryptoName: String,
    val cryptoSymbol: String,
    val cryptoAmount: Double,
    )