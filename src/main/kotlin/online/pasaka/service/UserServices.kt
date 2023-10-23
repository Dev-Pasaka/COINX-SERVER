package online.pasaka.service

import online.pasaka.repository.cryptodata.GetAllCryptoPrices
import online.pasaka.database.DatabaseConnection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.model.user.User
import online.pasaka.model.user.portfolio.LiveCryptoPrice
import online.pasaka.model.user.portfolio.LivePortfolio
import online.pasaka.model.wallet.Wallet
import org.litote.kmongo.*
import java.text.DecimalFormat


object UserServices {

    private val dbUser = DatabaseConnection.database.getCollection<User>()
    private val database: MongoDatabase = DatabaseConnection.database
    private val dbTraderWallet = database.getCollection<Wallet>()
    suspend fun createUser(userRegistration: User): Boolean {

        return coroutineScope {
            async { dbUser.insertOne(userRegistration).wasAcknowledged() }.await()
        }

    }

    suspend fun deleteAccount(email: String): Boolean {

        return coroutineScope {

            val deleteAccount = async {
                dbUser.findOneAndDelete(User::email eq email)
            }

            val deleteWallet = async {
                dbTraderWallet.findOneAndDelete(Wallet::walletId eq email)
            }

            val result = (deleteAccount.await() != null && deleteWallet.await() != null)
            result

        }
    }

    suspend fun createWallet(wallet: Wallet): Boolean {

        return coroutineScope {
            async { dbTraderWallet.insertOne(wallet).wasAcknowledged() }.await()
        }

    }

    suspend fun getUserData(email: String): User? {

        return coroutineScope {
            async { dbUser.findOne(User::email eq email) }.await()
        }

    }

    private suspend fun getUserPortfolio(email: String): Wallet? {

        return coroutineScope {
            async(Dispatchers.IO) { dbTraderWallet.findOne(Wallet::walletId eq email) }.await()
        }

    }

    suspend fun fetchUserCredentials(email: String): User? {

        return coroutineScope {
            async { dbUser.findOne(User::email eq email) }.await()
        }

    }

    suspend fun checkIfPhoneExists(phoneNumber: String): User? {

        return coroutineScope {
            async { dbUser.findOne(User::phoneNumber eq phoneNumber) }.await()
        }

    }

    suspend fun updatePasswordByPhoneNumber(phoneNumber: String, newPassword: String): UpdateResult? {

        return coroutineScope {
            async { dbUser.updateOne(User::phoneNumber eq phoneNumber, setValue(User::password, newPassword)) }.await()
        }

    }

    suspend fun liveUserPortfolio(email: String): LivePortfolio {

        return coroutineScope {

            val userPortfolio = async(Dispatchers.IO) { getUserPortfolio(email) }.await()
            val decimalFormat = DecimalFormat("#.##")
            var total = 0.0
            val cryptos = GetAllCryptoPrices().getAllCryptoMetadata()
            val portfolio = mutableListOf<LiveCryptoPrice>()

            userPortfolio?.assets?.forEach { coin ->

                run {

                    cryptos.forEach {

                        if (coin.symbol == it.symbol.replace(regex = Regex("\""), "")) {

                            println(coin.symbol)
                            println("crypto = ${coin.amount}, price = ${it.price}")
                            total += it.price.toString().toDouble() * coin.amount
                            println(total)
                            portfolio.add(
                                LiveCryptoPrice(
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

            LivePortfolio(
                balance = decimalFormat.format(total).toDouble(),
                assets = portfolio
            )

        }
    }
}
