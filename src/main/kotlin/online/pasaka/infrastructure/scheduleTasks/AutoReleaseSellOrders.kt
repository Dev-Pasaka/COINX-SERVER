package online.pasaka.infrastructure.scheduleTasks

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.ReleaseCrypto
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.model.cryptoAds.SellAd
import online.pasaka.domain.model.escrow.BuyEscrowWallet
import online.pasaka.domain.model.escrow.EscrowState
import online.pasaka.domain.model.escrow.SellEscrowWallet
import online.pasaka.domain.model.merchant.Merchant
import online.pasaka.domain.model.merchant.wallet.MerchantWallet
import online.pasaka.domain.model.order.BuyOrder
import online.pasaka.domain.model.order.OrderStatus
import online.pasaka.domain.model.order.SellOrder
import online.pasaka.domain.model.wallet.Wallet
import online.pasaka.domain.model.wallet.crypto.CryptoCoin
import online.pasaka.infrastructure.scheduleTasks.orderStatus.ExpireBuyOrders
import org.litote.kmongo.*

object AutoReleaseSellOrders {
    suspend fun autoReleaseSellOrders() {
        val gson = Gson()

        while (true) {

            ExpireBuyOrders.coroutineScope.launch {

                /** Get Orders in escrow wallet that seller have not released to the merchant and have no conflicts  */
                val expiredEscrowSellOrders = Entries.sellEscrowWallet.find(
                    and(
                        online.pasaka.domain.model.escrow.SellEscrowWallet::escrowState `in` listOf(
                            online.pasaka.domain.model.escrow.EscrowState.PENDING_SELLERS_RELEASE
                        ),
                        online.pasaka.domain.model.escrow.SellEscrowWallet::expiresAt lt System.currentTimeMillis(),

                        )
                ).toList()
                println(expiredEscrowSellOrders)

                expiredEscrowSellOrders.forEach {
                    /** Get Merchant's wallet*/
                    val merchantWallet = try {
                        async(Dispatchers.IO){
                            Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq it.sellersEmail)
                        }.await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    /** Update merchants wallet*/
                    val updateMerchantWallet = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
                    merchantWallet?.assets?.forEach { merchantAsset ->
                        if (merchantAsset.symbol == it.cryptoSymbol) {
                            updateMerchantWallet.add(
                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                    name = merchantAsset.name,
                                    symbol = merchantAsset.symbol,
                                    amount = merchantAsset.amount + it.cryptoAmount
                                )
                            )
                        } else {
                            updateMerchantWallet.add(
                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                    name = merchantAsset.name,
                                    symbol = merchantAsset.symbol,
                                    amount = merchantAsset.amount
                                )
                            )
                        }
                    }

                    /**Now add the escrow wallet data if it doesn't exist in the original assets*/
                    val escrowAsset = updateMerchantWallet.find { cryptoAsset -> cryptoAsset.symbol == it.cryptoSymbol }
                    if (escrowAsset == null) {
                        val escrowCryptoCoin = online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.cryptoSymbol,
                            amount = it.cryptoAmount,
                            name = it.cryptoName
                        )
                        updateMerchantWallet.add(escrowCryptoCoin)
                    }

                    /** Update seller's escrow wallet*/
                    val updateSellerEscrowWallet = try {
                        launch(Dispatchers.IO) {
                            Entries.sellEscrowWallet.updateOne(
                                online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq it.orderId,
                                it.copy(cryptoAmount = 0.0, escrowState = online.pasaka.domain.model.escrow.EscrowState.AUTO_RELEASED)
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }


                    /** Get merchants stats*/
                    val getMerchantOrderStats = try {
                        async(Dispatchers.IO) {
                            Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq it.merchantEmail )
                        }.await()
                    }catch (e:Exception){
                        e.printStackTrace()
                        null
                    }

                    /** Update merchant's stats*/
                    launch(Dispatchers.IO) {
                        if (getMerchantOrderStats != null){
                            Entries.dbMerchant.updateOne(
                                online.pasaka.domain.model.merchant.Merchant::email eq it.merchantEmail,
                                getMerchantOrderStats.copy(ordersCompleted = getMerchantOrderStats.ordersCompleted + 1)

                            )
                        }
                    }

                    /** Release crypto assets to the merchant and Notify him/her about the deposit */
                    try {
                        launch(Dispatchers.IO) {
                            val updatedAssets = merchantWallet?.copy(assets = updateMerchantWallet)
                            if (updatedAssets != null) {
                                Entries.userWallet.updateOne(
                                    online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq merchantWallet.walletId,
                                    updatedAssets
                                )
                            }
                        }
                        launch {
                            Entries.sellOrders.findOneAndUpdate(
                                online.pasaka.domain.model.order.SellOrder::orderId eq it.orderId,
                                setValue(online.pasaka.domain.model.order.SellOrder::orderStatus, online.pasaka.domain.model.order.OrderStatus.COMPLETED)
                            )
                        }
                        val notification = Notification(
                            notificationType = NotificationType.SELL_ORDER_COMPLETED,
                            notificationMessage = ReleaseCrypto(
                                orderId = it.orderId,
                                recipientEmail = merchantWallet?.walletId!!,
                                cryptoName = it.cryptoName,
                                cryptoSymbol = it.cryptoSymbol,
                                cryptoAmount = it.cryptoAmount,
                            )
                        )
                        launch(Dispatchers.IO) {
                            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, gson.toJson(notification))
                            println("Sent email notification")
                        }
                        println(it)

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }




                }

            }
            println("End of while loop")
            delay(1000)
        }
    }

}

