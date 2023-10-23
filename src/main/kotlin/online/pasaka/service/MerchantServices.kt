package online.pasaka.service

import online.pasaka.database.DatabaseConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.cryptoSwapListing.CryptoAssets
import online.pasaka.repository.cryptodata.GetCryptoPrice
import online.pasaka.rates.CreateCryptoAdsRate
import online.pasaka.model.cryptoAds.*
import online.pasaka.model.merchant.Merchant
import online.pasaka.model.merchant.wallet.MerchantAdsHistory
import online.pasaka.model.merchant.wallet.MerchantTopUpsHistory
import online.pasaka.model.merchant.wallet.MerchantWallet
import online.pasaka.model.merchant.wallet.MerchantsWithdrawalsHistory
import online.pasaka.model.merchant.wallet.crypto.CryptoSwap
import online.pasaka.model.user.PaymentMethod
import online.pasaka.model.user.User
import online.pasaka.model.wallet.crypto.CryptoCoin
import online.pasaka.responses.CreateBuyAdResult
import online.pasaka.responses.CreateSellAdResult
import online.pasaka.responses.DefaultResponse
import online.pasaka.responses.SwappingResults
import online.pasaka.utils.Utils
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne

object MerchantServices {

    private val dbUser = DatabaseConnection.database.getCollection<User>()
    private val dbMerchant = DatabaseConnection.database.getCollection<Merchant>()
    private val dbMerchantWallet = DatabaseConnection.database.getCollection<MerchantWallet>()
    private val dbMerchantsWithdrawalsHistory = DatabaseConnection.database.getCollection<MerchantsWithdrawalsHistory>()
    private val dbMerchantTopUpsHistory = DatabaseConnection.database.getCollection<MerchantTopUpsHistory>()
    private val dbCreateCreateCryptoBuyAd = DatabaseConnection.database.getCollection<CreateCryptoBuyAd>("buyAds")
    private val dbCreateSellAd = DatabaseConnection.database.getCollection<CreateCryptoSellAd>("sellAds")
    private val merchantAdHistory = DatabaseConnection.database.getCollection<MerchantAdsHistory>("AdsHistory")
    suspend fun becomeMerchant(email: String): String {

        return coroutineScope {

            val doesUserExist = async(Dispatchers.IO) {
                dbUser.findOne(User::email eq email)
            }.await() ?: return@coroutineScope "User doesn't exist"

            val doesMerchantExist = async {
                dbMerchant.findOne(Merchant::email eq email)
            }.await()

            if (doesMerchantExist != null) return@coroutineScope "Merchant already exist"

            val doesMerchantWalletExist = async(Dispatchers.IO) {
                dbMerchantWallet.findOne(MerchantWallet::walletId eq email)
            }.await()

            if (doesMerchantWalletExist != null) return@coroutineScope "Merchant Wallet Already Exists"

            val merchantWallet = MerchantWallet(
                walletId = doesUserExist.email,
                assets = listOf(
                    CryptoCoin(symbol = "USDT", name = "Tether", amount = 0.0),
                    CryptoCoin(symbol = "ETH", name = "Ethereum", amount = 0.0),
                    CryptoCoin(symbol = "Bitcoin", name = "Bitcoin", amount = 0.0),
                )
            )
            val merchant = Merchant(
                fullName = doesUserExist.fullName,
                username = doesUserExist.username,
                phoneNumber = doesUserExist.phoneNumber,
                email = doesUserExist.email,
                password = doesUserExist.password,
                ordersCompleted = 0,
                ordersCompletedByPercentage = 0,
                createdAt = Utils.currentTimeStamp(),
                country = "Kenya",
                kycVerification = false,
                paymentMethod = PaymentMethod()
            )
            val createMerchant = async { dbMerchant.insertOne(merchant).wasAcknowledged() }.await()
            val createMerchantWallet = async { dbMerchantWallet.insertOne(merchantWallet).wasAcknowledged() }.await()

            if (createMerchant && createMerchantWallet) {
                return@coroutineScope "Merchant registration was successful"
            } else {
                launch { dbMerchant.deleteOne(Merchant::email eq email) }
                launch { dbMerchantWallet.deleteOne(MerchantWallet::walletId eq email) }
                return@coroutineScope "Merchant registration failed"
            }

        }

    }


    suspend fun addMerchantPaymentMethod(email: String, paymentMethod: PaymentMethod): String {

        return coroutineScope {

            val existingMerchant = async {
                dbMerchant.findOne(User::email eq email)
            }.await() ?: return@coroutineScope "Merchant does not exist"


            val updatedPaymentMethod = PaymentMethod(
                mpesaSafaricom = paymentMethod.mpesaSafaricom
                    ?: existingMerchant.paymentMethod?.mpesaSafaricom,
                mpesaPaybill = paymentMethod.mpesaPaybill ?: existingMerchant.paymentMethod?.mpesaPaybill,
                mpesaTill = paymentMethod.mpesaTill ?: existingMerchant.paymentMethod?.mpesaTill
            )

            val addPaymentMethod = existingMerchant.copy(paymentMethod = updatedPaymentMethod)


            val updateMerchantPaymentMethod = async {
                dbMerchant.updateOne(User::email eq email, addPaymentMethod).wasAcknowledged()
            }.await()

            when (updateMerchantPaymentMethod) {
                true -> {
                    return@coroutineScope "Payment method added successfully"
                }

                else -> {
                    return@coroutineScope "Failed to add payment method"
                }
            }

        }
    }

    suspend fun merchantTopUpFloat(email: String, crypto: Double): String {

        return coroutineScope {

            val doesUserExist = async {
                dbMerchant.findOne(Merchant::email eq email)
            }.await() ?: return@coroutineScope "Merchant does not exist"

            val doesMerchantWalletExist = async {
                dbMerchantWallet.findOne(MerchantWallet::walletId eq email)
            }.await() ?: return@coroutineScope "Merchant wallet does not exist"

            val updatedAssets = mutableListOf<CryptoCoin>()
            doesMerchantWalletExist.assets.forEach {

                if (it.symbol == "USDT") {
                    val usdtAmount = it.amount
                    val updatedAmount = usdtAmount + crypto
                    updatedAssets.add(
                        CryptoCoin(symbol = it.symbol, name = it.name, amount = updatedAmount)
                    )
                } else {
                    updatedAssets.add(it)
                }

            }

            val updateMerchantData = doesMerchantWalletExist.copy(assets = updatedAssets)
            val topUpMerchantWallet = async {
                launch {
                    val topUpsHistory = MerchantTopUpsHistory(
                        fullName = doesUserExist.fullName,
                        userName = doesUserExist.username,
                        email = doesUserExist.email,
                        usdtAmount = crypto,
                        timeStamp = Utils.currentTimeStamp()
                    )
                    dbMerchantTopUpsHistory.insertOne(topUpsHistory)
                }
                dbMerchantWallet.updateOne(MerchantWallet::walletId eq email, updateMerchantData).wasAcknowledged()
            }.await()


            when (topUpMerchantWallet) {
                true -> return@coroutineScope "Merchant Wallet top up was successful"
                else -> return@coroutineScope "Merchant wallet top up was not successful"
            }

        }
    }

    suspend fun merchantWithdrawalFloat(email: String, crypto: Double): String {

        return coroutineScope {

            val doesUserExist = async {
                dbMerchant.findOne(Merchant::email eq email)
            }.await() ?: return@coroutineScope "Merchant does not exist"

            val doesMerchantWalletExist = async {
                dbMerchantWallet.findOne(MerchantWallet::walletId eq email)
            }.await() ?: return@coroutineScope "Merchant wallet does not exist"

            val updatedAssets = mutableListOf<CryptoCoin>()
            doesMerchantWalletExist.assets.forEach {

                if (it.symbol == "USDT") {
                    if (it.amount >= CreateCryptoAdsRate.withdrawalAmount(cryptoAmount = crypto)) {
                        val usdtAmount = it.amount
                        val updatedAmount = usdtAmount - CreateCryptoAdsRate.withdrawalAmount(cryptoAmount = crypto)
                        updatedAssets.add(
                            CryptoCoin(symbol = it.symbol, name = it.name, amount = updatedAmount)
                        )
                    } else return@coroutineScope "You have insufficient balance"
                } else {
                    updatedAssets.add(it)
                }

            }

            val updateMerchantData = doesMerchantWalletExist.copy(assets = updatedAssets)
            val topUpMerchantWallet = async {
                launch {
                    val topUpsHistory = MerchantsWithdrawalsHistory(
                        fullName = doesUserExist.fullName,
                        userName = doesUserExist.username,
                        email = doesUserExist.email,
                        usdtAmount = crypto,
                        timeStamp = Utils.currentTimeStamp()
                    )
                    dbMerchantsWithdrawalsHistory.insertOne(topUpsHistory)
                }
                dbMerchantWallet.updateOne(MerchantWallet::walletId eq email, updateMerchantData).wasAcknowledged()
            }.await()



            when (topUpMerchantWallet) {
                true -> return@coroutineScope "Merchant Wallet withdrawal was successful"
                else -> return@coroutineScope "Merchant wallet withdrawal was not successful"
            }

        }
    }

    suspend fun getMerchantFloatTopUpHistory(email: String): List<MerchantTopUpsHistory> {

        return coroutineScope {

            return@coroutineScope async(Dispatchers.IO) {
                dbMerchantTopUpsHistory.find().filter { it.email == email }
            }.await()

        }
    }

    suspend fun getMerchantFloatWithdrawalHistory(email: String): List<MerchantsWithdrawalsHistory> {

        return coroutineScope {

            return@coroutineScope async(Dispatchers.IO) {
                dbMerchantsWithdrawalsHistory.find().filter { it.email == email }
            }.await()

        }
    }

    suspend fun createBuyAd(cryptoBuyAdOrder: CryptoBuyAdOrder): CreateBuyAdResult {
        return coroutineScope {

            if (cryptoBuyAdOrder.minLimit >= cryptoBuyAdOrder.maxLimit)
                return@coroutineScope CreateBuyAdResult(

                    cryptoName = cryptoBuyAdOrder.cryptoName,
                    cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Minimum order limit should be less than maximum order limit"
                    )
                )
            if (cryptoBuyAdOrder.minLimit >= cryptoBuyAdOrder.totalAmount || cryptoBuyAdOrder.maxLimit >= cryptoBuyAdOrder.totalAmount)
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = cryptoBuyAdOrder.cryptoName,
                    cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Minimum order limit or maximum order limit can not be greater than total order cryptoAmount"
                    )
                )

            if (cryptoBuyAdOrder.margin > CreateCryptoAdsRate.maximumCreateBuyAdMargin)
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = cryptoBuyAdOrder.cryptoName,
                    cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Margin should not be greater that 0.05%"
                    )
                )

            val merchantData = try {
                async { dbMerchant.findOne(Merchant::email eq cryptoBuyAdOrder.email) }
            } catch (e: Throwable) {
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = cryptoBuyAdOrder.cryptoName,
                    cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Unexpected database error occurred"
                    )
                )
            }
            val merchantWallet = try {
                async { dbMerchantWallet.findOne(MerchantWallet::walletId eq cryptoBuyAdOrder.email) }
            } catch (e: Throwable) {
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = cryptoBuyAdOrder.cryptoName,
                    cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Unexpected database error occurred"
                    )
                )
            }

            val merchantDataResult = merchantData.await() ?: return@coroutineScope CreateBuyAdResult(
                cryptoName = cryptoBuyAdOrder.cryptoName,
                cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                cryptoAmount = cryptoBuyAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = "Merchant does not exist"
                )
            )

            val merchantWalletResult = merchantWallet.await() ?: return@coroutineScope CreateBuyAdResult(
                cryptoName = cryptoBuyAdOrder.cryptoName,
                cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                cryptoAmount = cryptoBuyAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = "Merchant wallet does not exist"
                )
            )

            val doesCryptoAssetExist =
                merchantWalletResult.assets.find { it.symbol == cryptoBuyAdOrder.cryptoSymbol.uppercase() }
                    ?: return@coroutineScope CreateBuyAdResult(
                        cryptoName = cryptoBuyAdOrder.cryptoName,
                        cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                        cryptoAmount = cryptoBuyAdOrder.totalAmount,
                        message = DefaultResponse(
                            status = false,
                            message = """
                            You do not have this ${cryptoBuyAdOrder.cryptoName} in your assets
                             you can swap to that crypto and try creating the ad again.
                        """.trimIndent()
                        )
                    )
            val debitAssets = mutableListOf<CryptoCoin>()

            merchantWalletResult.assets.forEach {
                if (it.symbol == cryptoBuyAdOrder.cryptoSymbol.uppercase()) {
                    if (it.amount >= cryptoBuyAdOrder.totalAmount) {
                        val deductedCrypto = it.amount - cryptoBuyAdOrder.totalAmount
                        debitAssets.add(
                            CryptoCoin(
                                symbol = doesCryptoAssetExist.symbol,
                                name = doesCryptoAssetExist.name,
                                amount = deductedCrypto
                            )
                        )
                    } else {
                        return@coroutineScope CreateBuyAdResult(
                            cryptoName = doesCryptoAssetExist.name,
                            cryptoSymbol = doesCryptoAssetExist.symbol,
                            cryptoAmount = cryptoBuyAdOrder.totalAmount,
                            message = DefaultResponse(
                                status = false,
                                message = """
                            You have insufficient ${cryptoBuyAdOrder.cryptoName} in your assets
                            try swapping or buying more ${cryptoBuyAdOrder.cryptoName}.
                        """.trimIndent()
                            )
                        )
                    }
                } else {
                    debitAssets.add(
                        CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }
            }


            val merchantCryptoBuyAd = CreateCryptoBuyAd(
                merchantUsername = merchantDataResult.username,
                email = merchantDataResult.email,
                cryptoName = doesCryptoAssetExist.name,
                cryptoSymbol = doesCryptoAssetExist.symbol,
                totalAmount = cryptoBuyAdOrder.totalAmount,
                minLimit = cryptoBuyAdOrder.minLimit,
                maxLimit = cryptoBuyAdOrder.maxLimit,
                margin = cryptoBuyAdOrder.margin,
                adStatus = AdStatus(open = true),
            )

            val updateMerchantAssets = try {
                async {
                    dbMerchantWallet.updateOne(
                        MerchantWallet::walletId eq cryptoBuyAdOrder.email,
                        merchantWalletResult.copy(assets = debitAssets)
                    ).wasAcknowledged()
                }
            } catch (_: Throwable) {
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = doesCryptoAssetExist.name,
                    cryptoSymbol = doesCryptoAssetExist.symbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }

            val createCryptoBuyAd = try {
                async {
                    dbCreateCreateCryptoBuyAd.insertOne(
                        merchantCryptoBuyAd
                    ).wasAcknowledged()
                }
            } catch (_: Throwable) {
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = doesCryptoAssetExist.name,
                    cryptoSymbol = doesCryptoAssetExist.symbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }

            launch {
                merchantAdHistory.insertOne(
                    MerchantAdsHistory(
                        fullName = merchantDataResult.fullName,
                        userName = merchantDataResult.username,
                        email = merchantDataResult.email,
                        cryptoName = doesCryptoAssetExist.name,
                        cryptoSymbol = doesCryptoAssetExist.symbol,
                        cryptoAmount = cryptoBuyAdOrder.totalAmount,
                        adType = "Buy",
                        createdAt = Utils.currentTimeStamp()

                    )
                )
            }

            val updateMerchantAssetsResult = updateMerchantAssets.await()
            val createCryptoBuyAdResult = createCryptoBuyAd.await()

            if (updateMerchantAssetsResult && createCryptoBuyAdResult)
                return@coroutineScope CreateBuyAdResult(
                    cryptoName = doesCryptoAssetExist.name,
                    cryptoSymbol = doesCryptoAssetExist.symbol,
                    cryptoAmount = cryptoBuyAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = true,
                        message = """
                            ${doesCryptoAssetExist.name} buy ad was created successfully .
                        """.trimIndent()
                    )
                )
            else return@coroutineScope CreateBuyAdResult(
                cryptoName = doesCryptoAssetExist.name,
                cryptoSymbol = doesCryptoAssetExist.symbol,
                cryptoAmount = cryptoBuyAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = """
                            Failed to create ${doesCryptoAssetExist.name} buy ad .
                        """.trimIndent()
                )
            )

        }
    }

    suspend fun createSellAd(cryptoSellAdOrder: CryptoSellAdOrder): CreateSellAdResult {

        return coroutineScope {

            if (cryptoSellAdOrder.minLimit >= cryptoSellAdOrder.maxLimit)
                return@coroutineScope CreateSellAdResult(
                    cryptoName = cryptoSellAdOrder.cryptoName,
                    cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Minimum order limit should be less than maximum order limit"
                    )
                )
            if (cryptoSellAdOrder.minLimit >= cryptoSellAdOrder.totalAmount || cryptoSellAdOrder.maxLimit >= cryptoSellAdOrder.totalAmount)
                return@coroutineScope CreateSellAdResult(
                    cryptoName = cryptoSellAdOrder.cryptoName,
                    cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Minimum order limit or maximum order limit can not be greater than total order cryptoAmount"
                    )
                )

            if (cryptoSellAdOrder.margin > CreateCryptoAdsRate.maximumCreateBuyAdMargin)
                return@coroutineScope CreateSellAdResult(
                    cryptoName = cryptoSellAdOrder.cryptoName,
                    cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Margin should not be greater that 0.05%"
                    )
                )

            val merchantData = try {
                async { dbMerchant.findOne(Merchant::email eq cryptoSellAdOrder.email) }
            } catch (e: Throwable) {
                return@coroutineScope CreateSellAdResult(
                    cryptoName = cryptoSellAdOrder.cryptoName,
                    cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Unexpected database error occurred"
                    )
                )
            }

            val merchantWallet = try {
                async { dbMerchantWallet.findOne(MerchantWallet::walletId eq cryptoSellAdOrder.email) }
            } catch (e: Throwable) {
                return@coroutineScope CreateSellAdResult(
                    cryptoName = cryptoSellAdOrder.cryptoName,
                    cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "Unexpected database error occurred"
                    )
                )
            }

            val merchantDataResult = merchantData.await() ?: return@coroutineScope CreateSellAdResult(
                cryptoName = cryptoSellAdOrder.cryptoName,
                cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                cryptoAmount = cryptoSellAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = "Merchant does not exist"
                )
            )

            val merchantWalletResult = merchantWallet.await() ?: return@coroutineScope CreateSellAdResult(
                cryptoName = cryptoSellAdOrder.cryptoName,
                cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                cryptoAmount = cryptoSellAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = "Merchant wallet does not exist"
                )
            )

            val doesCryptoAssetExist =
                merchantWalletResult.assets.find { it.symbol == cryptoSellAdOrder.cryptoSymbol.uppercase() }
                    ?: return@coroutineScope CreateSellAdResult(
                        cryptoName = cryptoSellAdOrder.cryptoName,
                        cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                        cryptoAmount = cryptoSellAdOrder.totalAmount,
                        message = DefaultResponse(
                            status = false,
                            message = """
                            You do not have this ${cryptoSellAdOrder.cryptoName} in your assets
                             you can swap to that crypto and try creating the ad again.
                        """.trimIndent()
                        )
                    )
            val debitAssets = mutableListOf<CryptoCoin>()

            merchantWalletResult.assets.forEach {
                if (it.symbol == cryptoSellAdOrder.cryptoSymbol.uppercase()) {
                    if (it.amount >= cryptoSellAdOrder.totalAmount) {
                        val deductedCrypto = it.amount - cryptoSellAdOrder.totalAmount
                        debitAssets.add(
                            CryptoCoin(
                                symbol = doesCryptoAssetExist.symbol,
                                name = doesCryptoAssetExist.name,
                                amount = deductedCrypto
                            )
                        )
                    } else {
                        return@coroutineScope CreateSellAdResult(
                            cryptoName = doesCryptoAssetExist.name,
                            cryptoSymbol = doesCryptoAssetExist.symbol,
                            cryptoAmount = cryptoSellAdOrder.totalAmount,
                            message = DefaultResponse(
                                status = false,
                                message = """
                            You have insufficient ${cryptoSellAdOrder.cryptoName} in your assets
                            try swapping or buying more ${cryptoSellAdOrder.cryptoName}.
                        """.trimIndent()
                            )
                        )
                    }
                } else {
                    debitAssets.add(
                        CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }
            }


            val merchantCryptoSellAd = CreateCryptoSellAd(
                merchantUsername = merchantDataResult.username,
                email = merchantDataResult.email,
                cryptoName = doesCryptoAssetExist.name,
                cryptoSymbol = doesCryptoAssetExist.symbol,
                totalAmount = cryptoSellAdOrder.totalAmount,
                margin = cryptoSellAdOrder.margin,
                minLimit = cryptoSellAdOrder.minLimit,
                maxLimit = cryptoSellAdOrder.maxLimit,
                adStatus = AdStatus(open = true),
            )

            val updateMerchantAssets = try {
                async {
                    dbMerchantWallet.updateOne(
                        MerchantWallet::walletId eq cryptoSellAdOrder.email,
                        merchantWalletResult.copy(assets = debitAssets)
                    ).wasAcknowledged()
                }
            } catch (_: Exception) {
                return@coroutineScope CreateSellAdResult(
                    cryptoName = doesCryptoAssetExist.name,
                    cryptoSymbol = doesCryptoAssetExist.symbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }

            val createCryptoBuyAd = try {
                async {
                    dbCreateSellAd.insertOne(
                        merchantCryptoSellAd
                    ).wasAcknowledged()
                }
            } catch (_: Exception) {
                return@coroutineScope CreateSellAdResult(
                    cryptoName = doesCryptoAssetExist.name,
                    cryptoSymbol = doesCryptoAssetExist.symbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }

            launch {
                merchantAdHistory.insertOne(
                    MerchantAdsHistory(
                        fullName = merchantDataResult.fullName,
                        userName = merchantDataResult.username,
                        email = merchantDataResult.email,
                        cryptoName = doesCryptoAssetExist.name,
                        cryptoSymbol = doesCryptoAssetExist.symbol,
                        cryptoAmount = cryptoSellAdOrder.totalAmount,
                        adType = "Sell",
                        createdAt = Utils.currentTimeStamp()

                    )
                )
            }

            val updateMerchantAssetsResult = updateMerchantAssets.await()
            val createCryptoBuyAdResult = createCryptoBuyAd.await()

            if (updateMerchantAssetsResult && createCryptoBuyAdResult)
                return@coroutineScope CreateSellAdResult(
                    cryptoName = doesCryptoAssetExist.name,
                    cryptoSymbol = doesCryptoAssetExist.symbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = true,
                        message = """
                            ${doesCryptoAssetExist.name} sell add was created successfully .
                        """.trimIndent()
                    )
                )
            else return@coroutineScope CreateSellAdResult(
                cryptoName = doesCryptoAssetExist.name,
                cryptoSymbol = doesCryptoAssetExist.symbol,
                cryptoAmount = cryptoSellAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = """
                            Failed to create ${doesCryptoAssetExist.name} sell add .
                        """.trimIndent()
                )
            )

        }
    }

    suspend fun swapCrypto(cryptoSwap: CryptoSwap): SwappingResults {

        return coroutineScope {

            val isToCryptoSwappable = try {
                async {
                    CryptoAssets.swappableCryptoAssets.find { cryptoSwap.toCryptoSymbol.uppercase() == it.symbol }
                        ?: "Not available"
                }
            } catch (_: Throwable) {
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }

            val isFromCryptoSwappable = try {
                async {
                    CryptoAssets.swappableCryptoAssets.find { cryptoSwap.fromCryptoSymbol.uppercase() == it.symbol }
                        ?: "Not available"
                }
            } catch (_: Throwable) {
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }


            if (isFromCryptoSwappable.await() == "Not available")
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "The crypto asset about to swap from is not available for swapping"
                    )
                )
            if (isToCryptoSwappable.await() == "Not available")
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "The crypto asset about to swap to is not available for swapping"
                    )
                )

            val getMerchantWallet = try {
                async {
                    dbMerchantWallet.findOne(MerchantWallet::walletId eq cryptoSwap.email)
                }.await() ?: return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "Wallet not found"
                    )
                )
            } catch (_: Throwable) {
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }

            println("Available assets : ${getMerchantWallet.assets}")
            val isToCryptoAssetAvailable =
                getMerchantWallet.assets.find { it.symbol == cryptoSwap.toCryptoSymbol.uppercase() }

            getMerchantWallet.assets.find { it.symbol == cryptoSwap.fromCryptoSymbol.uppercase() }
                ?: return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "You do not have ${cryptoSwap.fromCryptoSymbol} in your crypto assets to swap from"
                    )
                )
            val merchantsAssets = mutableListOf<CryptoCoin>()


            val getCryptoDataTo = async {
                GetCryptoPrice().getCryptoMetadata(cryptoSymbol = cryptoSwap.toCryptoSymbol)
            }

            val getCryptoDataFrom = async {
                GetCryptoPrice().getCryptoMetadata(cryptoSymbol = cryptoSwap.fromCryptoSymbol)
            }


            val getCryptoPriceTo =
                getCryptoDataTo.await().price?.toDoubleOrNull() ?: return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "Network error"
                    )
                )
            val getCryptoPriceFrom =
                getCryptoDataFrom.await().price?.toDoubleOrNull() ?: return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "Network error"
                    )
                )

            if (isToCryptoAssetAvailable == null) {
                merchantsAssets.add(
                    CryptoCoin(
                        symbol = getCryptoDataTo.await().symbol.replace("\"", ""),
                        name = getCryptoDataTo.await().name.replace("\"", ""),
                        amount = 0.0
                    )
                )
            }


            println("${cryptoSwap.toCryptoSymbol} price: $getCryptoPriceTo")


            val buyingPrice = (getCryptoPriceTo * cryptoSwap.cryptoAmount)
            println("Buying price: $buyingPrice")


            val swappingPrice = buyingPrice + CreateCryptoAdsRate.swapAmount(amountInUSD = buyingPrice)

            println("Swapping price $swappingPrice")

            val merchantCryptoAvailableCryptoAssets = getMerchantWallet.assets
            merchantCryptoAvailableCryptoAssets.forEach {
                merchantsAssets.add(
                    CryptoCoin(
                        symbol = it.symbol,
                        name = it.name,
                        amount = it.amount
                    )
                )
            }

            println("Merchant assets $merchantsAssets")

            val deductFromCryptoAsset = mutableListOf<CryptoCoin>()

            merchantsAssets.forEach {

                if (cryptoSwap.fromCryptoSymbol.uppercase() == it.symbol) {
                    val amount = it.amount
                    if (getCryptoPriceFrom * amount >= swappingPrice) {
                        val fromCryptoAmountUsd = getCryptoPriceFrom * it.amount
                        println("Available cryptoAmount $fromCryptoAmountUsd")
                        val cryptoAfterDeductionInUsd = (fromCryptoAmountUsd - swappingPrice)
                        val availableCrypto = cryptoAfterDeductionInUsd / getCryptoPriceFrom
                        deductFromCryptoAsset.add(
                            CryptoCoin(
                                symbol = it.symbol,
                                name = it.name,
                                amount = availableCrypto
                            )
                        )
                        println("Amount in USD after Deduction $cryptoAfterDeductionInUsd")
                    } else {
                        println("From price  ${getCryptoPriceFrom * amount}")
                        return@coroutineScope SwappingResults(
                            message = DefaultResponse(
                                status = false,
                                message = "You have insufficient balance"
                            )
                        )
                    }

                } else {
                    deductFromCryptoAsset.add(
                        CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }


            }
            val creditToCryptoAsset = mutableListOf<CryptoCoin>()

            deductFromCryptoAsset.forEach {
                if (it.symbol == cryptoSwap.toCryptoSymbol.uppercase()) {
                    val existingAmount =
                        merchantsAssets.find { it.symbol == cryptoSwap.toCryptoSymbol.uppercase() }?.amount ?: 0.0
                    creditToCryptoAsset.add(
                        CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = cryptoSwap.cryptoAmount + existingAmount
                        )
                    )
                } else {
                    creditToCryptoAsset.add(
                        CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }
            }

            println("Debited from crypto assets $deductFromCryptoAsset")
            println("Credited to crypto assets $creditToCryptoAsset")

            val updateMerchantWallet = try{
                async {
                    dbMerchantWallet.updateOne(
                        MerchantWallet::walletId eq cryptoSwap.email,
                        getMerchantWallet.copy(assets = creditToCryptoAsset)
                    ).wasAcknowledged()
                }.await()
            }catch (_:Throwable){
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred"
                    )
                )
            }
            val creditedCryptoAsset = creditToCryptoAsset.find { it.symbol == cryptoSwap.toCryptoSymbol.uppercase() }
            if (updateMerchantWallet) return@coroutineScope SwappingResults(
                cryptoName = creditedCryptoAsset?.name,
                cryptoSymbol = creditedCryptoAsset?.symbol,
                cryptoAmount = cryptoSwap.cryptoAmount,
                fiatAmount = Utils.formatCurrency(amount = swappingPrice),
                message = DefaultResponse(
                    status = true,
                    message = "Swapping was successful"
                )
            )
            else return@coroutineScope SwappingResults(
                message = DefaultResponse(
                    status = false,
                    message = "Swapping was not successful"
                )
            )

        }

    }


}

suspend fun main() {

}

