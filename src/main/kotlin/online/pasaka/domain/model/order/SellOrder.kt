package online.pasaka.domain.model.order

import online.pasaka.domain.utils.Utils

data class SellOrder(
    val orderId: String,
    val adId: String,
    val sellerEmail: String,
    val cryptoName: String,
    val cryptoSymbol: String,
    val cryptoAmount: Double,
    val amountInKes: Double,
    val orderStatus: online.pasaka.domain.model.order.OrderStatus = online.pasaka.domain.model.order.OrderStatus.STARTED,
    val createdAt: String = Utils.currentTimeStamp(),
    val expiresAt: Long
)
