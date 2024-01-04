package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.order.SellOrder
@Serializable
data class GetSellOrderResponse(
    val status:Boolean,
    val message: SellOrder?
)
