package online.pasaka.domain.service.encryption

interface EncryptionRepository {
    fun hashPassword(password:String):String
    fun verifyHashedPassword(password: String,hashedPassword:String):Boolean
}