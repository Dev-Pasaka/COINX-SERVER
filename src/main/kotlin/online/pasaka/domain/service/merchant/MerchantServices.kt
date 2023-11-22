package online.pasaka.domain.service.merchant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.domain.cryptoSwapListing.CryptoAssets
import online.pasaka.domain.model.cryptoAds.AdStatus
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.rates.CreateCryptoAdsRate
import online.pasaka.domain.repository.remote.cryptodata.GetCryptoPrice
import online.pasaka.domain.responses.CreateBuyAdResult
import online.pasaka.domain.responses.CreateSellAdResult
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.responses.SwappingResults
import online.pasaka.domain.utils.Utils
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.updateOne

object MerchantServices {


    suspend fun becomeMerchant(email: String): String {

        return coroutineScope {

            val doesUserExist = async(Dispatchers.IO) {
                Entries.dbUser.findOne(online.pasaka.domain.model.user.User::email eq email)
            }.await() ?: return@coroutineScope "User doesn't exist"

            val doesMerchantExist = async(Dispatchers.IO) {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq email)
            }.await()

            if (doesMerchantExist != null) return@coroutineScope "Merchant already exist"

            val doesMerchantWalletExist = async(Dispatchers.IO) {
                Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq email)
            }.await()

            if (doesMerchantWalletExist != null) return@coroutineScope "Merchant Wallet Already Exists"

            val merchantWallet = online.pasaka.domain.model.merchant.wallet.MerchantWallet(
                walletId = doesUserExist.email,
                assets = listOf(
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                        symbol = "USDT",
                        name = "Tether",
                        amount = 0.0
                    ),
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                        symbol = "ETH",
                        name = "Ethereum",
                        amount = 0.0
                    ),
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                        symbol = "Bitcoin",
                        name = "Bitcoin",
                        amount = 0.0
                    ),
                )
            )
            val merchant = online.pasaka.domain.model.merchant.Merchant(
                fullName = doesUserExist.fullName,
                username = doesUserExist.username,
                phoneNumber = doesUserExist.phoneNumber,
                email = doesUserExist.email,
                password = doesUserExist.password,
                ordersCompleted = 0,
                createdAt = Utils.currentTimeStamp(),
                country = "Kenya",
                kycVerification = false,
                paymentMethod = online.pasaka.domain.model.user.PaymentMethod()
            )
            val createMerchant = async(Dispatchers.IO) { Entries.dbMerchant.insertOne(merchant).wasAcknowledged() }.await()
            val createMerchantWallet = async(Dispatchers.IO) { Entries.dbMerchantWallet.insertOne(merchantWallet).wasAcknowledged() }.await()

            if (createMerchant && createMerchantWallet) {
                return@coroutineScope "Merchant registration was successful"
            } else {
                launch(Dispatchers.IO) { Entries.dbMerchant.deleteOne(online.pasaka.domain.model.merchant.Merchant::email eq email) }
                launch(Dispatchers.IO) { Entries.dbMerchantWallet.deleteOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq email) }
                return@coroutineScope "Merchant registration failed"
            }

        }

    }


    suspend fun addMerchantPaymentMethod(email: String, paymentMethod: online.pasaka.domain.model.user.PaymentMethod): String {

        return coroutineScope {

            val existingMerchant = async(Dispatchers.IO) {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.user.User::email eq email)
            }.await() ?: return@coroutineScope "Merchant does not exist"



            val updatedPaymentMethod = online.pasaka.domain.model.user.PaymentMethod(
                mpesaSafaricom = paymentMethod.mpesaSafaricom
                    ?: existingMerchant.paymentMethod?.mpesaSafaricom,
                mpesaPaybill = paymentMethod.mpesaPaybill ?: existingMerchant.paymentMethod?.mpesaPaybill,
                mpesaTill = paymentMethod.mpesaTill ?: existingMerchant.paymentMethod?.mpesaTill
            )

            val addPaymentMethod = existingMerchant.copy(paymentMethod = updatedPaymentMethod)


            val updateMerchantPaymentMethod = async(Dispatchers.IO) {
                Entries.dbMerchant.updateOne(online.pasaka.domain.model.user.User::email eq email, addPaymentMethod).wasAcknowledged()
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
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq email)
            }.await() ?: return@coroutineScope "Merchant does not exist"

            if (!doesUserExist.kycVerification) return@coroutineScope "KYC verification is not complete."

            val doesMerchantWalletExist = async {
                Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq email)
            }.await() ?: return@coroutineScope "Merchant wallet does not exist"

            val updatedAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
            doesMerchantWalletExist.assets.forEach {

                if (it.symbol == "USDT") {
                    val usdtAmount = it.amount
                    val updatedAmount = usdtAmount + crypto
                    updatedAssets.add(
                        online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = updatedAmount
                        )
                    )
                } else {
                    updatedAssets.add(it)
                }

            }

            val updateMerchantData = doesMerchantWalletExist.copy(assets = updatedAssets)
            val topUpMerchantWallet = async {
                launch {
                    val topUpsHistory = online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory(
                        fullName = doesUserExist.fullName,
                        userName = doesUserExist.username,
                        email = doesUserExist.email,
                        usdtAmount = crypto,
                    )
                    Entries.dbMerchantTopUpsHistory.insertOne(topUpsHistory)
                }
                Entries.dbMerchantWallet.updateOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq email, updateMerchantData).wasAcknowledged()
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
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq email)
            }.await() ?: return@coroutineScope "Merchant does not exist"

            if (!doesUserExist.kycVerification) return@coroutineScope "KYC verification is not complete."


            val doesMerchantWalletExist = async {
                Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq email)
            }.await() ?: return@coroutineScope "Merchant wallet does not exist"

            val updatedAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
            doesMerchantWalletExist.assets.forEach {

                if (it.symbol == "USDT") {
                    if (it.amount >= CreateCryptoAdsRate.withdrawalAmount(cryptoAmount = crypto)) {
                        val usdtAmount = it.amount
                        val updatedAmount = usdtAmount - CreateCryptoAdsRate.withdrawalAmount(cryptoAmount = crypto)
                        updatedAssets.add(
                            online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                symbol = it.symbol,
                                name = it.name,
                                amount = updatedAmount
                            )
                        )
                    } else return@coroutineScope "You have insufficient balance"
                } else {
                    updatedAssets.add(it)
                }

            }

            val updateMerchantData = doesMerchantWalletExist.copy(assets = updatedAssets)
            val topUpMerchantWallet = async {
                launch {
                    val topUpsHistory = online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory(
                        fullName = doesUserExist.fullName,
                        userName = doesUserExist.username,
                        email = doesUserExist.email,
                        usdtAmount = crypto,
                    )
                    Entries.dbMerchantsWithdrawalsHistory.insertOne(topUpsHistory)
                }
                Entries.dbMerchantWallet.updateOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq email, updateMerchantData).wasAcknowledged()
            }.await()



            when (topUpMerchantWallet) {
                true -> return@coroutineScope "Merchant Wallet withdrawal was successful"
                else -> return@coroutineScope "Merchant wallet withdrawal was not successful"
            }

        }
    }

    suspend fun getMerchantFloatTopUpHistory(email: String): List<online.pasaka.domain.model.merchant.wallet.MerchantTopUpsHistory> {

        return coroutineScope {

            return@coroutineScope async(Dispatchers.IO) {
                Entries.dbMerchantTopUpsHistory.find().filter { it.email == email }
            }.await()

        }
    }

    suspend fun getMerchantFloatWithdrawalHistory(email: String): List<online.pasaka.domain.model.merchant.wallet.MerchantsWithdrawalsHistory> {

        return coroutineScope {

            return@coroutineScope async(Dispatchers.IO) {
                Entries.dbMerchantsWithdrawalsHistory.find().filter { it.email == email }
            }.await()

        }
    }

    suspend fun createBuyAd(cryptoBuyAdOrder: online.pasaka.domain.dto.cryptoAds.CreateBuyAdDto): CreateBuyAdResult {
        return coroutineScope {

            val doesUserExist = async {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq cryptoBuyAdOrder.email)
            }.await()

            if (doesUserExist?.kycVerification == false)
            return@coroutineScope CreateBuyAdResult(

                cryptoName = cryptoBuyAdOrder.cryptoName,
                cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                cryptoAmount = cryptoBuyAdOrder.totalAmount,
                message = DefaultResponse(
                    status = false,
                    message = "KYC verification is not complete."
                )
            )


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
                        message = "Margin should not be greater that 50%"
                    )
                )

            val merchantData = try {
                async { Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq cryptoBuyAdOrder.email) }
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
                async { Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq cryptoBuyAdOrder.email) }
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
            val debitAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()

            merchantWalletResult.assets.forEach {
                if (it.symbol == cryptoBuyAdOrder.cryptoSymbol.uppercase()) {
                    if (it.amount >= cryptoBuyAdOrder.totalAmount) {
                        val deductedCrypto = it.amount - cryptoBuyAdOrder.totalAmount
                        debitAssets.add(
                            online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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
                        online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }
            }


            val merchantCryptoBuyAd = online.pasaka.domain.model.cryptoAds.BuyAd(
                merchantUsername = merchantDataResult.username,
                email = merchantDataResult.email,
                cryptoName = doesCryptoAssetExist.name,
                cryptoSymbol = doesCryptoAssetExist.symbol,
                totalAmount = cryptoBuyAdOrder.totalAmount,
                minLimit = cryptoBuyAdOrder.minLimit,
                maxLimit = cryptoBuyAdOrder.maxLimit,
                margin = cryptoBuyAdOrder.margin,
                adStatus = AdStatus.OPEN,
            )

            val updateMerchantAssets = try {
                async {
                    Entries.dbMerchantWallet.updateOne(
                        online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq cryptoBuyAdOrder.email,
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
                    Entries.buyAd.insertOne(
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
                Entries.merchantAdHistory.insertOne(
                    online.pasaka.domain.model.merchant.wallet.MerchantAdsHistory(
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

    suspend fun createSellAd(cryptoSellAdOrder: online.pasaka.domain.dto.cryptoAds.CreateSellAdDto): CreateSellAdResult {

        return coroutineScope {

            val doesUserExist = async {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq cryptoSellAdOrder.email)
            }.await()

            if (doesUserExist?.kycVerification == false)
                return@coroutineScope CreateSellAdResult(

                    cryptoName = cryptoSellAdOrder.cryptoName,
                    cryptoSymbol = cryptoSellAdOrder.cryptoSymbol,
                    cryptoAmount = cryptoSellAdOrder.totalAmount,
                    message = DefaultResponse(
                        status = false,
                        message = "KYC verification is not complete"
                    )
                )

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
                        message = "Margin should not be greater that 50%"
                    )
                )

            val merchantData = try {
                async { Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq cryptoSellAdOrder.email) }
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
                async { Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq cryptoSellAdOrder.email) }
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
            val debitAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()

            merchantWalletResult.assets.forEach {
                if (it.symbol == cryptoSellAdOrder.cryptoSymbol.uppercase()) {
                    if (it.amount >= cryptoSellAdOrder.totalAmount) {
                        val deductedCrypto = it.amount - cryptoSellAdOrder.totalAmount
                        debitAssets.add(
                            online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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
                        online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }
            }


            val merchantCryptoSellAd = online.pasaka.domain.model.cryptoAds.SellAd(
                merchantUsername = merchantDataResult.username,
                email = merchantDataResult.email,
                cryptoName = doesCryptoAssetExist.name,
                cryptoSymbol = doesCryptoAssetExist.symbol,
                totalAmount = cryptoSellAdOrder.totalAmount,
                margin = cryptoSellAdOrder.margin,
                minLimit = cryptoSellAdOrder.minLimit,
                maxLimit = cryptoSellAdOrder.maxLimit,
                adStatus = AdStatus.OPEN,
            )

            val updateMerchantAssets = try {
                async {
                    Entries.dbMerchantWallet.updateOne(
                        online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq cryptoSellAdOrder.email,
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
                    Entries.dbCreateSellAd.insertOne(
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
                Entries.merchantAdHistory.insertOne(
                    online.pasaka.domain.model.merchant.wallet.MerchantAdsHistory(
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

    suspend fun swapCrypto(cryptoSwap: online.pasaka.domain.model.merchant.wallet.crypto.CryptoSwap): SwappingResults {

        return coroutineScope {

            val doesUserExist = async {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq cryptoSwap.email)
            }.await()

            if (doesUserExist?.kycVerification == false)
                return@coroutineScope SwappingResults(
                    message = DefaultResponse(
                        status = false,
                        message = "KYC verification is not complete"
                    )
                )

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
                    Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq cryptoSwap.email)
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
            val merchantsAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()


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
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                        symbol = it.symbol,
                        name = it.name,
                        amount = it.amount
                    )
                )
            }

            println("Merchant assets $merchantsAssets")

            val deductFromCryptoAsset = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()

            merchantsAssets.forEach {

                if (cryptoSwap.fromCryptoSymbol.uppercase() == it.symbol) {
                    val amount = it.amount
                    if (getCryptoPriceFrom * amount >= swappingPrice) {
                        val fromCryptoAmountUsd = getCryptoPriceFrom * it.amount
                        println("Available cryptoAmount $fromCryptoAmountUsd")
                        val cryptoAfterDeductionInUsd = (fromCryptoAmountUsd - swappingPrice)
                        val availableCrypto = cryptoAfterDeductionInUsd / getCryptoPriceFrom
                        deductFromCryptoAsset.add(
                            online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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
                        online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = it.amount
                        )
                    )
                }


            }
            val creditToCryptoAsset = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()

            deductFromCryptoAsset.forEach {
                if (it.symbol == cryptoSwap.toCryptoSymbol.uppercase()) {
                    val existingAmount =
                        merchantsAssets.find { it.symbol == cryptoSwap.toCryptoSymbol.uppercase() }?.amount ?: 0.0
                    creditToCryptoAsset.add(
                        online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.symbol,
                            name = it.name,
                            amount = cryptoSwap.cryptoAmount + existingAmount
                        )
                    )
                } else {
                    creditToCryptoAsset.add(
                        online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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
                    Entries.dbMerchantWallet.updateOne(
                        online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq cryptoSwap.email,
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



