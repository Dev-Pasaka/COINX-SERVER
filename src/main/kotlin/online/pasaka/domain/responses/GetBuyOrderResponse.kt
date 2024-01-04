package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.order.BuyOrder

@Serializable
data class GetBuyOrderResponse(
    val status:Boolean,
    val message:BuyOrder?
)
