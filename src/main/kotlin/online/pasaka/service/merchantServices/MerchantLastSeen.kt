package online.pasaka.service.merchantServices

import online.pasaka.database.Entries
import online.pasaka.model.merchant.Merchant
import online.pasaka.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.setValue

suspend fun updateLastSeenStatus(email: String, lastSeen: String): DefaultResponse {
    return runCatching {
        val update = setValue(Merchant::lastSeen, lastSeen)
        Entries.dbMerchant.findOneAndUpdate(Merchant::email eq email, update)
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
