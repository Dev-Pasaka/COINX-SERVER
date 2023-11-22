package online.pasaka.infrastructure.scheduleTasks.orderStatus

import com.google.gson.Gson
import kotlinx.coroutines.*
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.OrderExpired
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.infrastructure.threads.Threads
import org.litote.kmongo.*
import java.util.concurrent.Executors

object ExpireBuyOrders {
    private val customDispatcher = Executors.newSingleThreadExecutor { r ->
        Thread(r, Threads.TASK_SCHEDULERS)
    }.asCoroutineDispatcher()

    private val gson = Gson()
    val coroutineScope = CoroutineScope(customDispatcher)
    suspend fun updateExpiredOrders() {

        while (true) {

            coroutineScope.launch {

                /** Get pending and expired Orders and Update orderStatus to EXPIRED and rollback transaction */
                val expiredOrders = Entries.cryptoBuyOrders.find(
                    and(
                        online.pasaka.domain.model.order.BuyOrder::orderStatus `in` listOf(
                            online.pasaka.domain.model.order.OrderStatus.PENDING,
                        ),
                        online.pasaka.domain.model.order.BuyOrder::expiresAt lt System.currentTimeMillis(),

                        )
                ).toList()

                expiredOrders.forEach {
                    val merchantInfo = try {
                        Entries.buyAd.findOne( online.pasaka.domain.model.cryptoAds.BuyAd::id eq it.adId)
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }

                    try {
                        launch(Dispatchers.IO) {
                            Entries.cryptoBuyOrders.updateOne(
                                online.pasaka.domain.model.order.BuyOrder::orderId eq it.orderId,
                                it.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.EXPIRED)
                            )
                        }
                        launch(Dispatchers.IO) {
                            transferMoneyBackFromEscrowToMerchant(orderId = it.orderId, adId = it.adId)
                        }
                        val notification = Notification(
                            notificationType = NotificationType.BUY_ORDER_EXPIRED,
                            notificationMessage = OrderExpired(
                                title = "Buy Order has expired",
                                orderId = it.orderId,
                                recipientName= merchantInfo?.merchantUsername!!,
                                recipientEmail = merchantInfo.email,
                                cryptoName = it.cryptoName,
                                cryptoSymbol = it.cryptoSymbol,
                                cryptoAmount = it.cryptoAmount,
                                amountInKes = it.amountInKes
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

    private suspend fun transferMoneyBackFromEscrowToMerchant(orderId: String, adId: String): DefaultResponse {
        return coroutineScope {
            async(Dispatchers.IO) {

                /** Get merchant assets in escrow wallet */
                val merchantCryptoAssetsInEscrow = try {
                    Entries.buyEscrowWallet.findOne(online.pasaka.domain.model.escrow.BuyEscrowWallet::orderId eq orderId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                    ?: return@async DefaultResponse(message = "Order not found in escrow wallet. Kindly Contact support for more information")

                /** Update merchant's assets in escrow to zero and Escrow state to Expired */
                val updateEscrowWallet = try {
                    Entries.buyEscrowWallet.updateOne(
                        online.pasaka.domain.model.escrow.BuyEscrowWallet::orderId eq orderId,
                        merchantCryptoAssetsInEscrow.copy(cryptoAmount = 0.0, escrowState = online.pasaka.domain.model.escrow.EscrowState.EXPIRED)
                    ).wasAcknowledged()
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }
                if (!updateEscrowWallet) return@async DefaultResponse(message = "An expected error occurred while debiting from escrow")

                /** Get merchant buy Crypto Ad */
                val getMerchantAd = try {
                    Entries.buyAd.findOne(online.pasaka.domain.model.cryptoAds.BuyAd::id eq adId)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
                    ?: return@async DefaultResponse(message = "Crypto Buy ad not found. Kindly contact support for more information")

                /** Credit assets from merchant's escrow wallet to his Crypto Ad */
                val creditCryptoBuyAd = try {
                    Entries.buyAd.updateOne(
                        online.pasaka.domain.model.cryptoAds.BuyAd::id eq adId, getMerchantAd.copy(
                            totalAmount = getMerchantAd.totalAmount + merchantCryptoAssetsInEscrow.cryptoAmount
                        )
                    ).wasAcknowledged()
                } catch (e: Exception) {
                    e.printStackTrace()
                    false
                }

                return@async if (creditCryptoBuyAd) DefaultResponse(
                    status = true,
                    message = "Crypto assets have successfully been credited to your cryptoAd from your escrow wallet"
                )
                else DefaultResponse("Failed to credit crypto assets to your crypto AD from your escrow wallet")


            }.await()
        }
    }

}


