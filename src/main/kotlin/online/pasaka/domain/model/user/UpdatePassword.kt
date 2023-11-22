package online.pasaka.domain.model.user

import kotlinx.serialization.Serializable

@Serializable
data class UpdatePassword(

        val phoneNumber: String,
        val newPassword:String,

)
