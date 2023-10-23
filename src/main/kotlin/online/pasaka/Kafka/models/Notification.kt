package online.pasaka.Kafka.models

import kotlinx.serialization.Serializable


data class Notification(
    val notificationType:NotificationType,
    val notificationMessage:Any
)
