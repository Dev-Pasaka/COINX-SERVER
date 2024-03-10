package online.pasaka.domain.repository.generateOtpRepository

import java.security.SecureRandom
import java.util.*

class GenerateOtpRepositoryImpl():GenerateOtpRepository {
    override suspend fun generateOtp(): String {
        val random = SecureRandom()
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        return  bytes.map { it.toInt() and 6 }.joinToString("")
    }
}

suspend fun main(){
println(    GenerateOtpRepositoryImpl().generateOtp())
}