package online.pasaka.service

import com.example.database.DatabaseConnection
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.UpdateResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.database.CrudOperations
import online.pasaka.model.user.User
import online.pasaka.model.wallet.Wallet
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.setValue


object UserServices{
    private val dbUser = CrudOperations.database.getCollection<User>()
    private val database: MongoDatabase = DatabaseConnection.database
    private val dbTraderWallet = database.getCollection<Wallet>()
    suspend fun createUser(userRegistration: User): Boolean {
        return coroutineScope {
            async { dbUser.insertOne(userRegistration).wasAcknowledged() }.await()

        }
    }

    suspend fun createWallet(wallet: Wallet): Boolean {
        return  coroutineScope {
          async { dbTraderWallet.insertOne(wallet).wasAcknowledged() }.await()
        }

    }

    suspend fun getUserPortfolio(email: String): Wallet? {
        return coroutineScope {
            async { dbTraderWallet.findOne(Wallet::walletId eq email) }.await()
        }
    }

    suspend fun getUserData(email: String): User? {
       return coroutineScope {
            async { dbUser.findOne(User::email eq email)  }.await()
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
            async {dbUser.updateOne(User::phoneNumber eq phoneNumber, setValue(User::password, newPassword)) }.await()
        }


    }
}
