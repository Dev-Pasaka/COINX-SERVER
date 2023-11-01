package online.pasaka.Kafka.models.messages

data class OrderExpired(
    val title:String = "Order Has Expired",
    val orderId:String,
    val recipientName:String,
    val recipientEmail: String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
    val amountInKes:Double
)
