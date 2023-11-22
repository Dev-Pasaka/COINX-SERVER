package online.pasaka.domain.service.orders.buyOrderService

import com.google.gson.Gson
import kotlinx.coroutines.*
import org.litote.kmongo.updateOne
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.OrderCancellation
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne

suspend fun cancelBuyOrder(buyOrderId: String): DefaultResponse {
    val gson = Gson()

    return coroutineScope {

        /** Does order exits */
        val doesBuyOrderExists = try {
            Entries.cryptoBuyOrders.findOne(online.pasaka.domain.model.order.BuyOrder::orderId eq buyOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist")


        /** Have you transferred funds yet */
        if (doesBuyOrderExists.orderStatus == online.pasaka.domain.model.order.OrderStatus.BUYER_HAS_TRANSFERRED_FUNDS)
            return@coroutineScope DefaultResponse(message = "Order can not be cancelled because you have already transferred funds")


        /** Update  order state*/
        val updateBuyOrder = doesBuyOrderExists.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.CANCELLED)
        launch(Dispatchers.IO) { Entries.cryptoBuyOrders.updateOne(online.pasaka.domain.model.order.BuyOrder::orderId eq buyOrderId, updateBuyOrder) }

        /** Return funds from escrow to merchants crypto ad*/
        val getMerchantsWalletFromEscrow = try {
            Entries.buyEscrowWallet.findOne(online.pasaka.domain.model.escrow.BuyEscrowWallet::orderId eq buyOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val getMerchantCryptoAd = try {
            val result = Entries.buyAd.findOne(online.pasaka.domain.model.cryptoAds.BuyAd::id eq doesBuyOrderExists.adId)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val creditMerchantCryptoAd = try {
            val updateAmount = getMerchantCryptoAd.totalAmount + getMerchantsWalletFromEscrow.cryptoAmount
            Entries.buyAd.updateOne(
                online.pasaka.domain.model.cryptoAds.BuyAd::id eq doesBuyOrderExists.adId,
                getMerchantCryptoAd.copy(totalAmount = updateAmount)
            ).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val deleteEscrowWallet = try {
            Entries.buyEscrowWallet.deleteOne(online.pasaka.domain.model.order.BuyOrder::orderId eq buyOrderId).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val notificationMessage = Notification(
            notificationType = NotificationType.BUY_ORDER_CANCELLED,
            notificationMessage = OrderCancellation(
                orderId = doesBuyOrderExists.orderId,
                recipientEmail = getMerchantsWalletFromEscrow.merchantEmail,
                recipientName = getMerchantsWalletFromEscrow.cryptoName,
                cryptoName = doesBuyOrderExists.cryptoName,
                cryptoSymbol = doesBuyOrderExists.cryptoSymbol,
                cryptoAmount = doesBuyOrderExists.cryptoAmount,
                amountInKes = doesBuyOrderExists.amountInKes
            )
        )
        launch(Dispatchers.IO) {
            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, message = gson.toJson(notificationMessage))
        }

        if (deleteEscrowWallet && creditMerchantCryptoAd) DefaultResponse(
            status = true,
            message = "Order has been cancelled"
        )
        else DefaultResponse()

        /** Notify merchant about order cancellation*/


    }

}

