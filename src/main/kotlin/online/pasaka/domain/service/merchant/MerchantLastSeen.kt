package online.pasaka.domain.service.merchant

import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

suspend fun updateLastSeenStatus(email: String, lastSeen: String): DefaultResponse {
    return runCatching {
        val update = setValue(online.pasaka.domain.model.merchant.Merchant::lastSeen, lastSeen)
        Entries.dbMerchant.findOneAndUpdate(online.pasaka.domain.model.merchant.Merchant::email eq email, update)
            ?: throw Exception("Failed to update last seen status")
    }.fold(
        onSuccess = {
            DefaultResponse(
                status = true,
                message = "Last seen status was updated successfully"
            )
        },
        onFailure = {
            DefaultResponse(
                message = "Failed to update last seen status: ${it.message}"
            )
        }
    )
}
