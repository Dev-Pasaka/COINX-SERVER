package online.pasaka.service

import com.example.database.DatabaseConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.model.merchant.Merchant
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.model.merchant.wallet.MerchantWallet
import online.pasaka.model.merchant.wallet.MerchantsWithdrawalsHistory
import online.pasaka.model.user.PaymentMethod
import online.pasaka.model.user.User
import online.pasaka.model.wallet.crypto.CryptoCoin
import online.pasaka.utils.GetCurrentTime
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne

object MerchantServices{
    private val dbUser = DatabaseConnection.database.getCollection<User>()
    private val dbMerchant = DatabaseConnection.database.getCollection<Merchant>()
    private val dbMerchantWallet = DatabaseConnection.database.getCollection<MerchantWallet>()
    private val dbMerchantsWithdrawalsHistory = DatabaseConnection.database.getCollection<MerchantsWithdrawalsHistory>()
    private val dbMerchantTopUpsHistory = DatabaseConnection.database.getCollection<MerchantTopUpsHistory>()
    suspend fun becomeMerchant(email: String): String {
        return coroutineScope {
            println(email)
            val doesUserExist = async(Dispatchers.IO) {
                dbUser.findOne(User::email eq email)
            }
            val existingUser = doesUserExist.await()
            if (existingUser != null) {
                val merchant = Merchant(
                    id = existingUser.id,
                    fullName = existingUser.fullName,
                    username = existingUser.username,
                    email = existingUser.email,
                    phoneNumber = existingUser.phoneNumber,
                    password = existingUser.password,
                    createdAt = GetCurrentTime.currentTime(),
                    merchant = true
                )
                val merchantWallet = MerchantWallet(
                    walletId = existingUser.email,
                    assets = listOf(
                        CryptoCoin(symbol = "USDT", name = "Tether", amount = 0.0),
                        CryptoCoin(symbol = "ETH", name = "Ethereum", amount = 0.0),
                        CryptoCoin(symbol = "Bitcoin", name = "Bitcoin", amount = 0.0),
                    )
                )
                val doesMerchantAndMerchantWalletExist = try {
                    val findMerchant = async(Dispatchers.IO) {
                        dbMerchant.findOne(Merchant::email eq email)
                    }
                    val findWallet = async(Dispatchers.IO) {
                        dbMerchantWallet.findOne(MerchantWallet::walletId eq email)
                    }
                    if (findMerchant.await() == null && findWallet.await() == null) {
                        "merchant and wallet does not exist"
                    } else if (findMerchant.await() != null || findWallet.await() != null) {
                        "merchant or wallet exists"
                    } else ""
                } catch (e: Exception) {
                    "an error occurred"
                }

                when (doesMerchantAndMerchantWalletExist) {
                    "merchant and wallet does not exist" -> {
                        val createMerchant = async(Dispatchers.IO) {
                            dbMerchant.insertOne(merchant).wasAcknowledged()
                        }
                        val createMerchantWallet = async(Dispatchers.IO) {
                            dbMerchantWallet.insertOne(merchantWallet).wasAcknowledged()
                        }
                        return@coroutineScope if (createMerchant.await() && createMerchantWallet.await()) {
                            "Merchant registration was successful"
                        } else {
                            val deleteMerchant = async(Dispatchers.IO) {
                                dbMerchant.deleteOne(Merchant::email eq email).wasAcknowledged()
                            }
                            val deleteMerchantWallet =
                                async(Dispatchers.IO) { dbMerchantWallet.deleteOne(MerchantWallet::walletId eq email) }
                            deleteMerchant.await()
                            deleteMerchantWallet.await()
                            "Merchant registration failed"
                        }
                    }

                    "merchant or wallet exists" -> {
                        return@coroutineScope "Merchant already exists"
                    }

                    else -> {
                        return@coroutineScope "Merchant registration failed"
                    }
                }

            } else {
                return@coroutineScope "User doesn't exist"
            }
        }

    }

    suspend fun addMerchantPaymentMethod(email: String, paymentMethod: PaymentMethod): String {
        return coroutineScope {
            val updatedPaymentMethod: PaymentMethod
            val existingMerchant = async(Dispatchers.IO) { dbMerchant.findOne(User::email eq email) }
            val existingMerchantResult = existingMerchant.await()
            var addPaymentMethod: Merchant? = null
            if (existingMerchantResult != null) {
                updatedPaymentMethod = PaymentMethod(
                    mpesaSafaricom = paymentMethod.mpesaSafaricom
                        ?: existingMerchantResult.paymentMethod?.mpesaSafaricom,
                    mpesaPaybill = paymentMethod.mpesaPaybill ?: existingMerchantResult.paymentMethod?.mpesaPaybill,
                    mpesaTill = paymentMethod.mpesaTill ?: existingMerchantResult.paymentMethod?.mpesaTill
                )
                addPaymentMethod = existingMerchantResult.copy(paymentMethod = updatedPaymentMethod)
            }
            if (addPaymentMethod != null) {
                val updateMerchantPaymentMethod = async(Dispatchers.IO) {
                    dbMerchant.updateOne(User::email eq email, addPaymentMethod).wasAcknowledged()
                }
                val updateMerchantPaymentMethodResult = updateMerchantPaymentMethod.await()
                if (updateMerchantPaymentMethodResult) return@coroutineScope "Payment method added successfully"
                else return@coroutineScope "Failed to add payment method"
            } else ""
        }
    }

    suspend fun merchantTopUpFloat(email: String, amount: Double, currency: String): String {
        return coroutineScope {
            // Launch multiple database queries concurrently using async
            val doesMerchantExist = async(Dispatchers.IO) {
                dbMerchant.findOne(Merchant::email eq email)
            }
            val getMerchantWallet = async(Dispatchers.IO) {
                dbMerchantWallet.findOne(MerchantWallet::walletId eq email)?.copy()
            }
            // Wait for the results of the coroutines
            val doesMerchantExistResult = doesMerchantExist.await()
            val doesWalletExistResult = getMerchantWallet.await()
            if (doesMerchantExistResult != null && doesWalletExistResult != null) {
                val doesCurrencyMatch = doesWalletExistResult.currency == currency.uppercase()
                if (doesCurrencyMatch) {
                    val updateMerchantFloat = doesWalletExistResult.copy(merchantFloat = amount+doesWalletExistResult.merchantFloat)
                    val topUpMerchantFloat = async(Dispatchers.IO) {
                        dbMerchantWallet.updateOne(MerchantWallet::walletId eq email, updateMerchantFloat)
                            .wasAcknowledged()
                    }
                    val topUpsHistory = MerchantTopUpsHistory(
                        fullName = doesMerchantExistResult.fullName,
                        userName = doesMerchantExistResult.username,
                        email = doesMerchantExistResult.email,
                        currency = doesWalletExistResult.currency,
                        amount = amount,
                        totalBalance = amount + doesWalletExistResult.merchantFloat,
                        timeStamp = GetCurrentTime.currentTime()
                    )
                    async(Dispatchers.IO) {
                        dbMerchantTopUpsHistory.insertOne(topUpsHistory)
                    }.await()
                    val topUpMerchantFloatResult = topUpMerchantFloat.await()

                    if (topUpMerchantFloatResult) {
                        return@coroutineScope "Float top-up was successful"
                    } else return@coroutineScope "Float top-up was not successful"
                } else return@coroutineScope "Currency is not supported, use USD"
            } else return@coroutineScope "Merchant does not exist"

        }
    }

    suspend fun merchantWithdrawalFloat(email: String, amount: Double, currency: String):String{
        return coroutineScope {
            // Launch multiple database queries concurrently using async
            val doesMerchantExist = async(Dispatchers.IO) {
                dbMerchant.findOne(Merchant::email eq email)
            }
            val getMerchantWallet = async(Dispatchers.IO) {
                dbMerchantWallet.findOne(MerchantWallet::walletId eq email)
            }
            // Wait for the results of the coroutines
            val doesMerchantExistResult = doesMerchantExist.await()
            val doesWalletExistResult = getMerchantWallet.await()
            if (doesMerchantExistResult != null && doesWalletExistResult != null) {
                val doesCurrencyMatch = doesWalletExistResult.currency == currency.uppercase()
                if (doesCurrencyMatch) {
                    if (doesWalletExistResult.merchantFloat >= amount){
                        val updateMerchantFloat = doesWalletExistResult.copy(merchantFloat = doesWalletExistResult.merchantFloat - amount)
                        val withdrawMerchantFloat = async(Dispatchers.IO) {
                            dbMerchantWallet.updateOne(MerchantWallet::walletId eq email, updateMerchantFloat)
                                .wasAcknowledged()
                        }
                        val withdrawalHistory = MerchantsWithdrawalsHistory(
                            fullName = doesMerchantExistResult.fullName,
                            userName = doesMerchantExistResult.username,
                            email = doesMerchantExistResult.email,
                            currency = doesWalletExistResult.currency,
                            amount = amount,
                            totalBalance = doesWalletExistResult.merchantFloat - amount,
                            timeStamp = GetCurrentTime.currentTime()
                        )
                        async(Dispatchers.IO) {
                            dbMerchantsWithdrawalsHistory.insertOne(withdrawalHistory)
                        }.await()
                        val topUpMerchantFloatResult = withdrawMerchantFloat.await()

                        if (topUpMerchantFloatResult) {
                            return@coroutineScope "Float withdrawal was successful"
                        } else return@coroutineScope "Float withdrawal was not successful"
                    }else return@coroutineScope "You have insufficient balance"

                } else return@coroutineScope "Currency is not supported, use USD"
            } else return@coroutineScope "Merchant does not exist"

        }
    }

    suspend fun getMerchantFloatTopUpHistory(email: String = "dev.pasaka@gmail.com"):List<MerchantTopUpsHistory> {
        return coroutineScope {
            return@coroutineScope async(Dispatchers.IO){
                dbMerchantTopUpsHistory.find().filter{it.email == email }
            }.await()


        }
    }
    suspend fun getMerchantFloatWithdrawalHistory(email: String = "dev.pasaka@gmail.com"): List<MerchantsWithdrawalsHistory> {
        return coroutineScope {
            return@coroutineScope async(Dispatchers.IO){
                dbMerchantsWithdrawalsHistory.find().filter{it.email == email }
            }.await()
        }
    }

}