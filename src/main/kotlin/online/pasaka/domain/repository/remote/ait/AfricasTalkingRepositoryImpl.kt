package online.pasaka.domain.repository.remote.ait

import com.africastalking.AfricasTalking
import com.africastalking.SmsService
import io.ktor.http.*
import io.ktor.server.config.*
import kotlinx.serialization.json.Json
import online.pasaka.domain.dto.merchant.AfricasTakingDto.AfricasTalkingSmsResponseDtoItem
import online.pasaka.domain.model.user.Otp
import online.pasaka.domain.responses.AfricasTalkingSmsResponse
import online.pasaka.infrastructure.config.Config

class AfricasTalkingRepositoryImpl(
    private val apiKey: String =  Config.load.tryGetString(key = "africastalking_api_key") ?: "",
    private val username: String  = Config.load.tryGetString(key = "africastalking_username") ?: ""
):AfricasTalkingRepository {
    override suspend fun sendOtp(otp: Otp): AfricasTalkingSmsResponse {
        AfricasTalking.initialize(username, apiKey)
        val sms = AfricasTalking.getService<SmsService>(AfricasTalking.SERVICE_SMS)
        return try {
            val response = sms.send(otp.otpCode, arrayOf(otp.toPhone), true)
            println(response)
            val resultObj = Json.decodeFromString<List<AfricasTalkingSmsResponseDtoItem>>(response.toString())
            println(resultObj)
            AfricasTalkingSmsResponse(
                httpStatusCode = resultObj.first().statusCode,
                serverStatus = true,
                errorMessage = if (resultObj.first().statusCode == 102) null else "Service is unavailable"
            )
        } catch (e: Exception) {
            println(e.printStackTrace())
            AfricasTalkingSmsResponse(
                httpStatusCode = HttpStatusCode.InternalServerError.value,
                serverStatus = true,
                errorMessage = "An internal server error has occurred"
            )
        }
    }

}
