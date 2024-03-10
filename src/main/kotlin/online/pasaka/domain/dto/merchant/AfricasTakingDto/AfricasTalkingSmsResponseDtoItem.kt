package online.pasaka.domain.dto.merchant.AfricasTakingDto


import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AfricasTalkingSmsResponseDtoItem(
    @SerialName("cost")
    val cost: String,
    @SerialName("messageId")
    val messageId: String,
    @SerialName("number")
    val number: String,
    @SerialName("status")
    val status: String,
    @SerialName("statusCode")
    val statusCode: Int
)