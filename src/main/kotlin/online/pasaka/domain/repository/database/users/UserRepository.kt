package online.pasaka.domain.repository.database.users

import com.mongodb.client.result.UpdateResult
import online.pasaka.domain.model.user.User
import online.pasaka.domain.model.user.portfolio.LivePortfolio
import online.pasaka.domain.model.wallet.Wallet

interface UserRepository {
    suspend fun createUser(userRegistration: User): Boolean
    suspend fun deleteAccount(email: String): Boolean
    suspend fun createWallet(wallet: Wallet): Boolean
    suspend fun getUserPortfolio(email: String): Wallet?
    suspend fun getUserData(email: String): User?
    suspend fun fetchUserCredentials(email: String): User?
    suspend fun checkIfPhoneExists(phoneNumber: String): User?
    suspend fun updatePassword(phoneNumber: String, newPassword: String): UpdateResult?
    suspend fun liveUserPortfolio(email: String): LivePortfolio
    suspend fun createOtpCode(email: String, otpCode:String):User?
    suspend fun resetPassword(email: String, newPassword: String): Boolean
}