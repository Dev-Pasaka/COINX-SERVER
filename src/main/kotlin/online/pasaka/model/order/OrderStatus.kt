package online.pasaka.model.order

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    STARTED,
    PENDING,
    BUYER_HAS_TRANSFERRED_FUNDS,
    COMPLETED,
    EXPIRED,
    CANCELLED
}