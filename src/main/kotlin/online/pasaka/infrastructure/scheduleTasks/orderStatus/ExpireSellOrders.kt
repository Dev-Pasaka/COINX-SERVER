package online.pasaka.infrastructure.scheduleTasks.orderStatus

import com.google.gson.Gson
import kotlinx.coroutines.*
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.OrderExpired
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.model.cryptoAds.BuyAd
import online.pasaka.domain.model.cryptoAds.SellAd
import online.pasaka.domain.model.escrow.EscrowState
import online.pasaka.domain.model.escrow.SellEscrowWallet
import online.pasaka.domain.model.order.OrderStatus
import online.pasaka.domain.model.order.SellOrder
import online.pasaka.domain.model.user.User
import online.pasaka.domain.model.wallet.Wallet
import online.pasaka.domain.model.wallet.crypto.CryptoCoin
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.infrastructure.threads.Threads
import org.litote.kmongo.*
import java.util.concurrent.Executors

object ExpireSellOrders {
    private val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.TASK_SCHEDULERS)
    }.asCoroutineDispatcher()

    private val gson = Gson()
    val coroutineScope = CoroutineScope(customDispatcher)
    suspend fun updateExpiredSellOrders() {

        while (true) {

            coroutineScope.launch {

                /** Get pending and expired Orders and Update orderStatus to EXPIRED and rollback transaction */
                val expiredOrders = Entries.sellOrders.find(
                    and(
                        online.pasaka.domain.model.order.SellOrder::orderStatus `in` listOf(
                            online.pasaka.domain.model.order.OrderStatus.PENDING,
                        ),
                       online.pasaka.domain.model.order.SellOrder::expiresAt lt System.currentTimeMillis(),

                        )
                ).toList()

                println(expiredOrders)

                 expiredOrders.forEach {
                     val merchantInfo = try {
                         Entries.sellAd.findOne( online.pasaka.domain.model.cryptoAds.SellAd::id eq it.adId)
                     } catch (e: Exception) {
                         e.printStackTrace()
                         null
                     }

                     /** GetMerchants crypto ad*/
                     val merchantsCryptoAd = try {
                         async(Dispatchers.IO) {
                             Entries.sellAd.findOne(online.pasaka.domain.model.cryptoAds.SellAd::id eq it.adId)
                         }.await()
                     } catch (e: Exception) {
                         null
                     }

                     /** Credit merchants cryptoAd*/
                     val debitMerchantCryptoAd = try {
                         if (merchantsCryptoAd != null){
                             async(Dispatchers.IO) {
                                 Entries.sellAd
                                     .updateOne(
                                         online.pasaka.domain.model.cryptoAds.SellAd::id eq it.adId,
                                         merchantsCryptoAd.copy(totalAmount = merchantsCryptoAd.totalAmount + it.cryptoAmount)
                                     )
                                     .wasAcknowledged()
                             }
                         }else{}
                     } catch (e: Exception) {
                         e.printStackTrace()
                         null
                     }

                    try {
                        launch(Dispatchers.IO) {
                            Entries.sellOrders.updateOne(
                                online.pasaka.domain.model.order.SellOrder::orderId eq it.orderId,
                                it.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.EXPIRED)
                            )
                        }
                        launch(Dispatchers.IO) {
                            transferMoneyBackFromEscrowToSellerWallet(orderId = it.orderId, walletId = it.sellerEmail)
                        }
                        val notification = Notification(
                            notificationType = NotificationType.SELL_ORDER_EXPIRED,
                            notificationMessage = OrderExpired(
                                title = "Sell Order Has Expired",
                                orderId = it.orderId,
                                recipientName= merchantInfo?.merchantUsername ?: "",
                                recipientEmail = merchantInfo?.email ?:"",
                                cryptoName = it.cryptoName,
                                cryptoSymbol = it.cryptoSymbol,
                                cryptoAmount = it.cryptoAmount,
                                amountInKes = it.amountInKes
                            )
                        )
                        println("Notification otpCode: $notification")
                        launch(Dispatchers.IO) {
                            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, gson.toJson(notification))
                        }

                        println(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }


                }

            }
            delay(10000)
        }
    }

    private suspend fun transferMoneyBackFromEscrowToSellerWallet(orderId: String, walletId: String): DefaultResponse {
        return coroutineScope {
            async(Dispatchers.IO) {

                /** Get merchant assets in escrow wallet */
                val getSellersAssetsInEscrow = try {
                    Entries.sellEscrowWallet.findOne(online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq orderId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                    ?: return@async DefaultResponse(message = "Order not found in escrow wallet. Kindly Contact support for more information")

                /** Update seller's assets in escrow to zero and Escrow state to Expired */
                val updateEscrowWallet = try {
                    Entries.sellEscrowWallet.updateOne(
                        online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq orderId,
                        getSellersAssetsInEscrow.copy(cryptoAmount = 0.0, escrowState = online.pasaka.domain.model.escrow.EscrowState.EXPIRED)
                    ).wasAcknowledged()
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                if (!updateEscrowWallet) return@async DefaultResponse(message = "An expected error occurred while debiting from escrow")

                /** Get sellersWallet */
                val getSellersWallet = try {
                    Entries.userWallet.findOne(online.pasaka.domain.model.wallet.Wallet::walletId eq walletId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                    ?: return@async DefaultResponse(message = "Wallet not found. Kindly contact support for more information")

                /** Update sellers wallet **/
                val updateSellersAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
                getSellersWallet.assets.forEach {
                    if (it.symbol == getSellersAssetsInEscrow.cryptoSymbol){
                        updateSellersAssets.add(
                            online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                symbol = it.symbol,
                                name = it.name,
                                amount = it.amount + getSellersAssetsInEscrow.cryptoAmount
                            )
                        )
                    }else{
                        updateSellersAssets.add(
                            online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                symbol = it.symbol,
                                name = it.name,
                                amount = it.amount
                            )
                        )
                    }
                }

                /** Credit assets from seller's escrow wallet to his wallet */
                val creditSellersWallet = try {
                    Entries.userWallet.updateOne(
                        online.pasaka.domain.model.wallet.Wallet::walletId eq  walletId,
                        getSellersWallet.copy(assets = updateSellersAssets)
                    ).wasAcknowledged()
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }

                return@async if (creditSellersWallet) DefaultResponse(
                    status = true,
                    message = "Crypto assets have successfully been credited to your cryptoAd from your escrow wallet"
                )
                else DefaultResponse("Failed to credit crypto assets to your crypto AD from your escrow wallet")


            }.await()
        }
    }

}

