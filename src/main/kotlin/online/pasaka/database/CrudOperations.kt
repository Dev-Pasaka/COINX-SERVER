package com.example.database

import online.pasaka.model.user.User
import online.pasaka.model.wallet.Wallet
import com.mongodb.client.MongoDatabase
import com.mongodb.client.result.UpdateResult
import org.litote.kmongo.*

object CrudOperations {

    private val database: MongoDatabase = DatabaseConnection.database

    private val dbWallet  = database.getCollection<Wallet>()
    private val dbUser = database.getCollection<User>()


    fun createUser(userRegistration: User): Boolean{
        return dbUser.insertOne(userRegistration).wasAcknowledged()
    }
    fun createWallet(wallet: Wallet): Boolean{
        return dbWallet.insertOne(wallet).wasAcknowledged()
    }

    fun getUserPortfolio(email: String): Wallet? {

        return dbWallet.findOne(Wallet::walletId eq email)

    }

    fun getUserData(email:String): User?{
        return dbUser.findOne(User::email eq email)
    }

    fun fetchUserCredentials(email: String): User?{
        return dbUser.findOne(User::email eq email)
    }

    fun checkIfPhoneExists(phoneNumber:String) :User?{
        return  dbUser.findOne(User::phoneNumber eq phoneNumber)
    }
    fun updatePasswordByPhoneNumber(phoneNumber: String, newPassword: String): UpdateResult?{
       val result = dbUser.updateOne(User::phoneNumber eq phoneNumber, setValue(User::password, newPassword))
        println("$result ------------------------------------------------------------")
        return result
    }


}