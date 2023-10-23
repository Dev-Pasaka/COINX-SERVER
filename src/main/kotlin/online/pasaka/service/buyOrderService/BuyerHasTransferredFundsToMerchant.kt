package online.pasaka.service.buyOrderService

import kotlinx.coroutines.coroutineScope
import online.pasaka.database.DatabaseConnection
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.model.wallet.Wallet
import online.pasaka.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne






suspend fun buyerHasTransferredFundsToMerchant(buyOrderID: String): DefaultResponse {

    val buyEscrowWallet = DatabaseConnection.database.getCollection<BuyEscrowWallet>("EscrowWallet")
    val cryptoBuyOrders = DatabaseConnection.database.getCollection<BuyOrder>("BuyOrders")
    val buyersWallet = DatabaseConnection.database.getCollection<Wallet>()

    return coroutineScope {

        val doesBuyOrderExists = try {
            cryptoBuyOrders.findOne(BuyOrder::orderId eq buyOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist")

        /** Update Buy order state */
        val updateOrderStatus = try {
            cryptoBuyOrders.updateOne(
                BuyOrder::orderId eq buyOrderID,
                doesBuyOrderExists.copy(orderStatus = OrderStatus.BUYER_HAS_TRANSFERRED_FUNDS)
            ).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")


        /** Notify Merchant about the Order State*/

        DefaultResponse(status = true, message = "Message Kindly wait for merchant to confirm and release crypto")

    }

}