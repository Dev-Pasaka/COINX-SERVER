package online.pasaka.domain.repository.remote.ait

import online.pasaka.domain.model.user.Otp
import online.pasaka.domain.responses.AfricasTalkingSmsResponse

interface AfricasTalkingRepository {

    suspend fun sendOtp(otp:Otp):AfricasTalkingSmsResponse
}