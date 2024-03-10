package online.pasaka.domain.service.passwordResetService

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import online.pasaka.domain.model.user.Otp
import online.pasaka.domain.repository.database.users.UserRepositoryImpl
import online.pasaka.domain.repository.generateOtpRepository.GenerateOtpRepositoryImpl
import online.pasaka.domain.repository.remote.ait.AfricasTalkingRepositoryImpl
import online.pasaka.domain.responses.AfricasTalkingSmsResponse
import online.pasaka.domain.responses.PasswordResetResponse

class PasswordResetService() {

    private suspend fun generateOtp(): String = GenerateOtpRepositoryImpl().generateOtp()
    suspend fun sendOtp(phoneNumber: String): AfricasTalkingSmsResponse {
        val otp = generateOtp()
        val user = UserRepositoryImpl().checkIfPhoneExists(phoneNumber = phoneNumber)
            ?: return AfricasTalkingSmsResponse(
                httpStatusCode = 200,
                serverStatus = true,
                errorMessage = "User phone number does not exit"
            )

        val otpMessage = """
            Subject: Password Reset OTP

            Dear ${user.username},

            You have requested to reset your password. Please use the following One-Time Password (OTP) to complete the reset process:

            OTP: $otp

            If you did not request this password reset, please ignore this message. Ensure that you keep your OTP confidential and do not share it with anyone.

            Thank you,
            Coinx
           

        """.trimIndent()

        val createOpt = UserRepositoryImpl().createOtpCode(phoneNumber = phoneNumber, otp)
        return when (createOpt != null) {
            true -> AfricasTalkingRepositoryImpl().sendOtp(
                otp = Otp(
                    toPhone = user.phoneNumber,
                    otpCode = otpMessage
                )
            )

            false -> AfricasTalkingSmsResponse(
                httpStatusCode = 200,
                serverStatus = true,
                errorMessage = "Failed to send Otp code"
            )
        }

    }

    suspend fun resendOtp(phoneNumber: String): AfricasTalkingSmsResponse {
        val otp = generateOtp()
        val user = UserRepositoryImpl().checkIfPhoneExists(phoneNumber = phoneNumber)
            ?: return AfricasTalkingSmsResponse(
                httpStatusCode = 200,
                serverStatus = true,
                errorMessage = "User phone number does not exit"
            )


        val otpMessage = """
            Subject: Password Reset OTP Resend

            Dear ${user.username},

            You recently requested to reset your password on Coinx. If you didn't make this request, please disregard this email.
            
            Please use the following One-Time Password (OTP) to complete the password reset process:

            OTP: $otp   

            This OTP is valid for a short period. Please reset your password promptly.
            
            Thank you for choosing Coinx,
            
            The Coinx Team
           

        """.trimIndent()

        val createOpt = UserRepositoryImpl().createOtpCode(phoneNumber = phoneNumber, otp)
        return when (createOpt != null) {
            true -> AfricasTalkingRepositoryImpl().sendOtp(
                otp = Otp(
                    toPhone = user.phoneNumber,
                    otpCode = otpMessage
                )
            )

            false -> AfricasTalkingSmsResponse(
                httpStatusCode = 200,
                serverStatus = true,
                errorMessage = "Failed to resend Otp code"
            )
        }
    }

    private suspend fun verifyOtpCode(phoneNumber: String, otpCode: String): Boolean = withContext(context = Dispatchers.IO) {
        return@withContext UserRepositoryImpl().getUserData(phoneNumber)?.otpCode == otpCode
    }

    suspend fun resetPassword(phoneNumber: String, otpCode: String, newPassword: String): PasswordResetResponse  = withContext(Dispatchers.IO){
        val isOtpValid = verifyOtpCode(phoneNumber, otpCode)

        if (!isOtpValid)  return@withContext PasswordResetResponse(status = false, message = "Invalid otp code")

        val isPasswordResetSuccessful = UserRepositoryImpl().resetPassword(phoneNumber = phoneNumber, newPassword = newPassword )
        if (!isPasswordResetSuccessful)  PasswordResetResponse(status = false, message = "Failed to reset Password")

        PasswordResetResponse(status = true, message = "Password Reset was successful")


    }


}

suspend fun main() {
    println(PasswordResetService().sendOtp(phoneNumber = "+254717722324"))
   //println(PasswordResetService().resetPassword(phoneNumber = "+254717722324", "442022", newPassword = "Pasaka001"))
}