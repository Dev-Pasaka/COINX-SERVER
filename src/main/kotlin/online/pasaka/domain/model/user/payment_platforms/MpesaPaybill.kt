package online.pasaka.domain.model.user.payment_platforms

import kotlinx.serialization.Serializable

@Serializable
data class MpesaPaybill(
    var businessNumber:String = "",
    var accountNumber:String = "",
    )