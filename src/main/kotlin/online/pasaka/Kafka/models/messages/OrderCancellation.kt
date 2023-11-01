package online.pasaka.Kafka.models.messages

data class OrderCancellation(
    val title:String = "Buyer Has Cancelled The Order",
    val orderId:String,
    val recipientName:String,
    val recipientEmail: String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
    val amountInKes:Double
)
