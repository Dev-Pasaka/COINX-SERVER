package online.pasaka.Kafka.models

enum class NotificationType {
    ORDER_HAS_BEEN_PLACED,
    BUYER_HAS_TRANSFERRED_FUNDS,
    COMPLETED,
    EXPIRED,
    CANCELLED
}