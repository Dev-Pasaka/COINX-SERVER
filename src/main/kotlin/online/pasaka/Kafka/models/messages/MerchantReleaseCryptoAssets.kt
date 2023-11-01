package online.pasaka.Kafka.models.messages

data class MerchantReleaseCryptoAssets(
    val title:String = "Deposit Was Successful",
    val orderId:String,
    val recipientEmail: String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val cryptoAmount:Double,
)
