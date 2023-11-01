package online.pasaka.Kafka.models.messages

data class FundsTransferNotification(
    val title:String = "Funds Transferred",
    val orderId:String,
    val recipientName:String,
    val recipientEmail: String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
    val amountInKes:Double
)
