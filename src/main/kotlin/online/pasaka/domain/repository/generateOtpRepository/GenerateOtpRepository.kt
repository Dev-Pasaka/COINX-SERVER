package online.pasaka.domain.repository.generateOtpRepository

interface GenerateOtpRepository {
    suspend fun generateOtp():String
}