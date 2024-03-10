package online.pasaka.domain.repository.database.users

import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.*
import online.pasaka.domain.model.user.User
import online.pasaka.domain.model.user.portfolio.LivePortfolio
import online.pasaka.domain.model.wallet.Wallet
import online.pasaka.infrastructure.database.Entries

import online.pasaka.domain.repository.remote.cryptodata.GetAllCryptoPrices
import online.pasaka.domain.service.encryption.EncryptionRepositoryImpl
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.setValue
import java.text.DecimalFormat

class UserRepositoryImpl(val entries: Entries = Entries) : UserRepository {
    override suspend fun createUser(userRegistration: User): Boolean  = withContext(Dispatchers.IO) {
        return@withContext async(Dispatchers.IO) { entries.dbUser.insertOne(userRegistration).wasAcknowledged() }.await()
    }

    override suspend fun deleteAccount(email: String): Boolean  = withContext(Dispatchers.IO){


            val deleteAccount = async(Dispatchers.IO) {
                entries.dbUser.findOneAndDelete(User::email eq email)
            }

            val deleteWallet = async(Dispatchers.IO) {
                entries.userWallet.findOneAndDelete(Wallet::walletId eq email)
            }

            val result = (deleteAccount.await() != null && deleteWallet.await() != null)

            return@withContext result


    }

    override suspend fun createWallet(wallet: Wallet): Boolean  = withContext(Dispatchers.IO){
        return@withContext async(Dispatchers.IO) { entries.userWallet.insertOne(wallet).wasAcknowledged() }.await()
    }

    override suspend fun getUserPortfolio(email: String): Wallet? = withContext(Dispatchers.IO) {
        return@withContext async(Dispatchers.IO) { entries.userWallet.findOne(Wallet::walletId eq email) }.await()
    }

    override suspend fun getUserData(phoneNumber: String): User? = withContext(Dispatchers.IO) {
        return@withContext async(Dispatchers.IO) { entries.dbUser.findOne(User::phoneNumber eq phoneNumber) }.await()
    }

    override suspend fun fetchUserCredentials(email: String): User? {
        return coroutineScope {
            async(Dispatchers.IO) { entries.dbUser.findOne(online.pasaka.domain.model.user.User::email eq email) }.await()
        }
    }

    override suspend fun checkIfPhoneExists(phoneNumber: String): User?  = withContext(Dispatchers.IO){
        return@withContext async(Dispatchers.IO) { entries.dbUser.findOne(User::phoneNumber eq phoneNumber) }.await()
    }

    override suspend fun updatePassword(email: String, newPassword: String): UpdateResult? {
        return coroutineScope {
            async(Dispatchers.IO) {
                entries.dbUser.updateMany(
                    User::email eq email,
                    listOf(
                        setValue(User::password, newPassword),
                        setValue(User::otpCode, null)
                    )
                )
            }.await()
        }
    }

    override suspend fun resetPassword(phoneNumber: String, newPassword: String): Boolean = withContext(Dispatchers.IO) {
        val hashedPassword = EncryptionRepositoryImpl().hashPassword(password = newPassword)
        launch {
            entries.dbUser.updateOne(
                User::phoneNumber eq phoneNumber,
                setValue(User::otpCode, null)
            ).wasAcknowledged()
        }
        return@withContext entries.dbUser.updateOne(
            User::phoneNumber eq phoneNumber,
            setValue(User::password, hashedPassword)
        ).wasAcknowledged()

    }

    override suspend fun liveUserPortfolio(email: String): LivePortfolio = withContext(Dispatchers.IO) {


            val userPortfolio = async(Dispatchers.IO) { getUserPortfolio(email) }.await()
            val decimalFormat = DecimalFormat("#.##")
            var total = 0.0
            val cryptos = GetAllCryptoPrices().getAllCryptoMetadata()
            val portfolio = mutableListOf<online.pasaka.domain.model.user.portfolio.LiveCryptoPrice>()

            userPortfolio?.assets?.forEach { coin ->

                run {
                    cryptos.forEach {

                        if (coin.symbol == it.symbol.replace(regex = Regex("\""), "")) {

                            println(coin.symbol)
                            println("crypto = ${coin.amount}, price = ${it.price}")
                            total += it.price.toString().toDouble() * coin.amount
                            println(total)
                            portfolio.add(
                                online.pasaka.domain.model.user.portfolio.LiveCryptoPrice(
                                    symbol = coin.symbol,
                                    name = coin.name,
                                    amount = coin.amount,
                                    marketPrice = decimalFormat.format(it.price.toString().toDouble() * coin.amount)
                                        .toDouble()
                                )
                            )

                        }
                    }
                }
            }

           return@withContext LivePortfolio(
                balance = decimalFormat.format(total).toDouble(),
                assets = portfolio
            )


    }

    override suspend fun createOtpCode(phoneNumber: String, otpCode: String): User? = withContext(Dispatchers.IO) {
        return@withContext entries.dbUser.findOneAndUpdate(
            User::phoneNumber eq phoneNumber,
            setValue(User::otpCode, otpCode)
        )
    }

}

suspend fun main() {
    println(
        UserRepositoryImpl().resetPassword(phoneNumber = "dev.pasaka@gmail.com", newPassword = "Pasaka001")
    )
}