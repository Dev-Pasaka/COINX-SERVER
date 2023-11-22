package online.pasaka.domain.model.order

import online.pasaka.domain.utils.Utils


data class BuyOrder(
    val orderId: String,
    val adId: String,
    val buyersEmail: String,
    val cryptoName: String,
    val cryptoSymbol: String,
    val cryptoAmount: Double,
    val amountInKes: Double,
    val orderStatus: OrderStatus = OrderStatus.STARTED,
    val createdAt: String = Utils.currentTimeStamp(),
    val expiresAt: Long

)
