package online.pasaka.scheduleTasks

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.MerchantReleaseCryptoAssets
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.database.Entries
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.escrow.EscrowState
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.model.wallet.Wallet
import online.pasaka.model.wallet.crypto.CryptoCoin
import online.pasaka.scheduleTasks.orderStatus.ExpiredOrders
import org.litote.kmongo.*
import kotlin.system.exitProcess

object AutoRelease {
    suspend fun autoReleaseBuyOrders() {
        val gson = Gson()

        while (true) {

            ExpiredOrders.coroutineScope.launch {

                /** Get Orders in escrow wallet that merchant have not released to the buyer and have no conflicts  */
                val expiredEscrowBuyOrders = Entries.buyEscrowWallet.find(
                    and(
                        BuyEscrowWallet::escrowState `in` listOf(
                            EscrowState.PENDING_MERCHANT_RELEASE
                        ),
                        BuyEscrowWallet::expiresAt lt System.currentTimeMillis(),

                        )
                ).toList()

                expiredEscrowBuyOrders.forEach {
                    /** Get buyers wallet*/
                    val buyersWallet = try {
                        Entries.dbTraderWallet.findOne(Wallet::walletId eq it.buyerEmail)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    /** Update buyers wallet*/
                    val updateBuyersAssets = mutableListOf<CryptoCoin>()
                    buyersWallet?.assets?.forEach { buyersAsset ->
                        if (buyersAsset.symbol == it.cryptoSymbol) {
                            updateBuyersAssets.add(
                                CryptoCoin(
                                    name = buyersAsset.name,
                                    symbol = buyersAsset.symbol,
                                    amount = buyersAsset.amount + it.cryptoAmount
                                )
                            )
                        } else {
                            updateBuyersAssets.add(
                                CryptoCoin(
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
                        val escrowCryptoCoin = CryptoCoin(
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
                                BuyEscrowWallet::orderId eq it.orderId,
                                it.copy(cryptoAmount = 0.0, escrowState = EscrowState.AUTO_RELEASED)
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
                                Entries.dbTraderWallet.updateOne(
                                    Wallet::walletId eq buyersWallet.walletId,
                                    updatedAssets
                                )
                            }
                        }
                        launch {
                            Entries.cryptoBuyOrders.findOneAndUpdate(
                                BuyOrder::orderId eq it.orderId,
                                setValue(BuyOrder::orderStatus, OrderStatus.COMPLETED)
                            )
                        }
                        val notification = Notification(
                            notificationType = NotificationType.BUYER_HAS_TRANSFERRED_FUNDS,
                            notificationMessage = MerchantReleaseCryptoAssets(
                                orderId = it.orderId,
                                recipientEmail = buyersWallet?.walletId!!,
                                cryptoName = it.cryptoName,
                                cryptoSymbol = it.cryptoSymbol,
                                cryptoAmount = it.cryptoAmount,
                            )
                        )
                        launch(Dispatchers.IO) {
                            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, gson.toJson(notification))
                        }

                        println(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                }

            }
            delay(1000)
        }
    }

}

suspend fun main() {
    AutoRelease.autoReleaseBuyOrders()
}