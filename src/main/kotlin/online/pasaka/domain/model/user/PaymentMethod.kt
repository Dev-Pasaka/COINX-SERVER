package online.pasaka.domain.model.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import online.pasaka.domain.model.user.payment_platforms.MpesaPaybill
import online.pasaka.domain.model.user.payment_platforms.MpesaSafaricom
import online.pasaka.domain.model.user.payment_platforms.MpesaTill
@Serializable
data class PaymentMethod(

    @SerialName("mpesaSafaricom")
    val mpesaSafaricom: MpesaSafaricom? = null,

    @SerialName("mpesaPaybill")
    var mpesaPaybill: MpesaPaybill? = null,

    @SerialName("mpesaTill")
    val mpesaTill: MpesaTill? = null
)

