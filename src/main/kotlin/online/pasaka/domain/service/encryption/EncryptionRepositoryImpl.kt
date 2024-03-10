package online.pasaka.domain.service.encryption

import org.mindrot.jbcrypt.BCrypt

class EncryptionRepositoryImpl():EncryptionRepository {
    override fun hashPassword(password: String): String  =
         BCrypt.hashpw(password, BCrypt.gensalt())

    override fun verifyHashedPassword(password:String,hashedPassword: String): Boolean  = BCrypt.checkpw(password, hashedPassword)
}