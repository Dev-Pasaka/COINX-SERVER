package online.pasaka.domain.dto.cryptoAds

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.cryptoAds.AdStatus
import online.pasaka.domain.model.user.PaymentMethod

@Serializable
data class GetSellAdDto(
    val id:String,
    val merchantUsername:String,
    val email:String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val minLimit:Double,
    val maxLimit:Double,
    val adStatus: AdStatus,
    val margin:Double,
    val cryptoPrice:Double,
    val createdAt:String,
    val ordersMade:Long = 0,
    val ordersCompleted:Long = 0,
    val paymentMethod: PaymentMethod?,
    val ordersCompletedByPercentage:Double = 0.0,
    val lastSeen:String

)