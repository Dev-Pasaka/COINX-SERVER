package online.pasaka.Kafka.models.messages

data class ReleaseCrypto(
    val title:String = "Deposit Was Successful",
    val orderId:String,
    val recipientEmail: String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
)
