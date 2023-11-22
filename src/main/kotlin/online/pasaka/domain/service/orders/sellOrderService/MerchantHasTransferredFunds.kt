package online.pasaka.domain.service.orders.sellOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.FundsTransferNotification
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.setValue
import org.litote.kmongo.updateOne

suspend fun merchantHasTransferredFunds(sellOrderID: String): DefaultResponse {
    val gson = Gson()

    return coroutineScope {

        val doesSellOrderExists = try {
            Entries.sellOrders.findOne(online.pasaka.domain.model.order.SellOrder::orderId eq sellOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist")

        when (doesSellOrderExists.orderStatus) {
            online.pasaka.domain.model.order.OrderStatus.CANCELLED -> return@coroutineScope DefaultResponse("This order is already cancelled, kindly check with customer service for more information.")
            online.pasaka.domain.model.order.OrderStatus.EXPIRED -> return@coroutineScope DefaultResponse("This order has expired, kindly check with customer service for more information.")
            online.pasaka.domain.model.order.OrderStatus.COMPLETED -> return@coroutineScope DefaultResponse("This Order is already completed. kindly check with customer service for more information.")
            online.pasaka.domain.model.order.OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS -> return@coroutineScope DefaultResponse("Merchant has already transferred the funds. kindly wait for seller to confirm the amount and release crypto.")
            else -> {}
        }
        val getSellersEscrowInfo = try {
            async(Dispatchers.IO) {
                Entries.sellEscrowWallet.findOne(online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq sellOrderID)
            }.await()
        }catch (e:Exception){
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Failed to seller's escrow data")

        val getSellersData = try {
            async(Dispatchers.IO) {  Entries.dbUser.findOne(online.pasaka.domain.model.user.User::email eq getSellersEscrowInfo.sellersEmail)}.await()
        }catch (e:Exception){
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto Ad info not found")

        /** Update Sell order state */
        val updateOrderStatus = try {
            Entries.sellOrders.updateOne(
                online.pasaka.domain.model.order.SellOrder::orderId eq sellOrderID,
                doesSellOrderExists.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS)
            ).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred when updating order state")

        /** Notify seller about the money transfer */
        val notificationMessage = Notification(
            notificationType = NotificationType.MERCHANT_HAS_TRANSFERRED_FUNDS,
            notificationMessage = FundsTransferNotification(
                orderId = doesSellOrderExists.orderId,
                recipientName = getSellersData.username,
                recipientEmail = getSellersData.email,
                cryptoName = doesSellOrderExists.cryptoName,
                cryptoSymbol = doesSellOrderExists.cryptoSymbol,
                cryptoAmount = doesSellOrderExists.cryptoAmount,
                amountInKes = doesSellOrderExists.amountInKes
            )
        )

        /** Update sellers escrowState */
        val escrowStateResult = launch(Dispatchers.IO) {
            val updateEscrowState = setValue(online.pasaka.domain.model.escrow.SellEscrowWallet::escrowState, online.pasaka.domain.model.escrow.EscrowState.PENDING_SELLERS_RELEASE)
            Entries.sellEscrowWallet.findOneAndUpdate(online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq sellOrderID, updateEscrowState )
        }

        launch(Dispatchers.IO) { kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, message = gson.toJson(notificationMessage)) }
        DefaultResponse(status = true, message = "Kindly wait for seller to confirm and release crypto")

    }

}

