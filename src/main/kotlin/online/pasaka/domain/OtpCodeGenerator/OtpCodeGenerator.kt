package online.pasaka.domain.OtpCodeGenerator

import java.security.SecureRandom

object OtpCodeGenerator {
    fun generateCode():String{

        val random = SecureRandom()
        val bytes = ByteArray(6)
        random.nextBytes(bytes)
        return  bytes.map { it.toInt() and 6 }.joinToString("")

    }
}