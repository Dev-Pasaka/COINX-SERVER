package online.pasaka.Kafka.models

import kotlinx.serialization.Serializable

data class BuyOrderConfirmationNotificationMessage(
    val title:String = "P2P Order Confirmation",
    val orderId:String,
    val iconUrl:String = "https://play-lh.googleusercontent.com/Yg7Lo7wiW-iLzcnaarj7nm5-hQjl7J9eTgEupxKzC79Vq8qyRgTBnxeWDap-yC8kHoE=w240-h480-rw",
    val recipientName:String,
    val recipientEmail: String,
    val cryptoName:String = "Tether",
    val cryptoSymbol:String = "USDT",
    val cryptoAmount:Double = 50.0,
    val amountInKes:Double
)
