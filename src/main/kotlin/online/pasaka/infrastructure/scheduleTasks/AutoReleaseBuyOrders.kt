package online.pasaka.infrastructure.scheduleTasks

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.ReleaseCrypto
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.model.escrow.BuyEscrowWallet
import online.pasaka.domain.model.escrow.EscrowState
import online.pasaka.domain.model.order.BuyOrder
import online.pasaka.domain.model.order.OrderStatus
import online.pasaka.domain.model.wallet.Wallet
import online.pasaka.domain.model.wallet.crypto.CryptoCoin
import online.pasaka.infrastructure.scheduleTasks.orderStatus.ExpireBuyOrders
import org.litote.kmongo.*

object AutoReleaseBuyOrders {
    suspend fun autoReleaseBuyOrders() {
        val gson = Gson()

        while (true) {

            ExpireBuyOrders.coroutineScope.launch {

                /** Get Orders in escrow wallet that merchant have not released to the buyer and have no conflicts  */
                val expiredEscrowBuyOrders = Entries.buyEscrowWallet.find(
                    and(
                        online.pasaka.domain.model.escrow.BuyEscrowWallet::escrowState `in` listOf(
                            online.pasaka.domain.model.escrow.EscrowState.PENDING_MERCHANT_RELEASE
                        ),
                        online.pasaka.domain.model.escrow.BuyEscrowWallet::expiresAt lt System.currentTimeMillis(),

                        )
                ).toList()
                println(expiredEscrowBuyOrders)

                expiredEscrowBuyOrders.forEach {
                    /** Get buyers wallet*/
                    val buyersWallet = try {
                        Entries.userWallet.findOne(online.pasaka.domain.model.wallet.Wallet::walletId eq it.buyerEmail)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    /** Update buyers wallet*/
                    val updateBuyersAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
                    buyersWallet?.assets?.forEach { buyersAsset ->
                        if (buyersAsset.symbol == it.cryptoSymbol) {
                            updateBuyersAssets.add(
                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                    name = buyersAsset.name,
                                    symbol = buyersAsset.symbol,
                                    amount = buyersAsset.amount + it.cryptoAmount
                                )
                            )
                        } else {
                            updateBuyersAssets.add(
                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                    name = buyersAsset.name,
                                    symbol = buyersAsset.symbol,
                                    amount = buyersAsset.amount
                                )
                            )
                        }
                    }

                    /**Now add the escrow wallet data if it doesn't exist in the original assets*/
                    val escrowAsset = updateBuyersAssets.find { cryptoAsset -> cryptoAsset.symbol == it.cryptoSymbol }
                    if (escrowAsset == null) {
                        val escrowCryptoCoin = online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                            symbol = it.cryptoSymbol,
                            amount = it.cryptoAmount,
                            name = it.cryptoName
                        )
                        updateBuyersAssets.add(escrowCryptoCoin)
                    }

                    /** Update merchant's escrow wallet*/
                    val updateMerchantEscrowWallet = try {
                        launch(Dispatchers.IO) {
                            Entries.buyEscrowWallet.updateOne(
                                online.pasaka.domain.model.escrow.BuyEscrowWallet::orderId eq it.orderId,
                                it.copy(cryptoAmount = 0.0, escrowState = online.pasaka.domain.model.escrow.EscrowState.AUTO_RELEASED)
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }


                    /** Release crypto assets to the buyer and Notify him about the deposit */
                    try {
                        launch(Dispatchers.IO) {
                            val updatedAssets = buyersWallet?.copy(assets = updateBuyersAssets)
                            if (updatedAssets != null) {
                                Entries.userWallet.updateOne(
                                    online.pasaka.domain.model.wallet.Wallet::walletId eq buyersWallet.walletId,
                                    updatedAssets
                                )
                            }
                        }
                        launch {
                            Entries.cryptoBuyOrders.findOneAndUpdate(
                                online.pasaka.domain.model.order.BuyOrder::orderId eq it.orderId,
                                setValue(online.pasaka.domain.model.order.BuyOrder::orderStatus, online.pasaka.domain.model.order.OrderStatus.COMPLETED)
                            )
                        }
                        val notification = Notification(
                            notificationType = NotificationType.BUY_ORDER_COMPLETED,
                            notificationMessage = ReleaseCrypto(
                                orderId = it.orderId,
                                recipientEmail = buyersWallet?.walletId!!,
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

