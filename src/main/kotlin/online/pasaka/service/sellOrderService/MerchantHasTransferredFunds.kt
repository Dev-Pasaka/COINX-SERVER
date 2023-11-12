package online.pasaka.service.sellOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.FundsTransferNotification
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.database.Entries
import online.pasaka.model.cryptoAds.BuyAd
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.escrow.EscrowState
import online.pasaka.model.escrow.SellEscrowWallet
import online.pasaka.model.merchant.Merchant
import online.pasaka.model.merchant.wallet.MerchantWallet
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.model.order.SellOrder
import online.pasaka.model.user.User
import online.pasaka.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.setValue
import org.litote.kmongo.updateOne

suspend fun merchantHasTransferredFunds(sellOrderID: String): DefaultResponse {
    val gson = Gson()

    return coroutineScope {

        val doesSellOrderExists = try {
            Entries.sellOrders.findOne(SellOrder::orderId eq sellOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist")

        when (doesSellOrderExists.orderStatus) {
            OrderStatus.CANCELLED -> return@coroutineScope DefaultResponse("This order is already cancelled, kindly check with customer service for more information.")
            OrderStatus.EXPIRED -> return@coroutineScope DefaultResponse("This order has expired, kindly check with customer service for more information.")
            OrderStatus.COMPLETED -> return@coroutineScope DefaultResponse("This Order is already completed. kindly check with customer service for more information.")
            OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS -> return@coroutineScope DefaultResponse("Merchant has already transferred the funds. kindly wait for seller to confirm the amount and release crypto.")
            else -> {}
        }
        val getSellersEscrowInfo = try {
            async(Dispatchers.IO) {
                Entries.sellEscrowWallet.findOne(SellEscrowWallet::orderId eq sellOrderID)
            }.await()
        }catch (e:Exception){
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Failed to seller's escrow data")

        val getSellersData = try {
            async(Dispatchers.IO) {  Entries.dbUser.findOne(User::email eq getSellersEscrowInfo.sellersEmail)}.await()
        }catch (e:Exception){
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto Ad info not found")

        /** Update Sell order state */
        val updateOrderStatus = try {
            Entries.sellOrders.updateOne(
                SellOrder::orderId eq sellOrderID,
                doesSellOrderExists.copy(orderStatus = OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS)
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
            val updateEscrowState = setValue(SellEscrowWallet::escrowState, EscrowState.PENDING_SELLERS_RELEASE)
            Entries.sellEscrowWallet.findOneAndUpdate(SellEscrowWallet::orderId eq sellOrderID, updateEscrowState )
        }

        launch(Dispatchers.IO) { kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, message = gson.toJson(notificationMessage)) }
        DefaultResponse(status = true, message = "Kindly wait for seller to confirm and release crypto")

    }

}

suspend fun main(){
    println(
        merchantHasTransferredFunds(
            sellOrderID = "6548d2508832442bcedf8edd"
        )
    )

}