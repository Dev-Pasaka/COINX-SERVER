package online.pasaka.service.sellOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.OrderCancellation
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.database.Entries
import online.pasaka.model.cryptoAds.BuyAd
import online.pasaka.model.cryptoAds.SellAd
import online.pasaka.model.escrow.SellEscrowWallet
import online.pasaka.model.merchant.Merchant
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.model.order.SellOrder
import online.pasaka.model.wallet.Wallet
import online.pasaka.model.wallet.crypto.CryptoCoin
import online.pasaka.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.updateOne

suspend fun cancelSellOrder(sellOrderId: String): DefaultResponse {
    val gson = Gson()

    return coroutineScope {

        /** Does order exits */
        val doesSellOrderExists = try {
            Entries.sellOrders.findOne(SellOrder::orderId eq sellOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto sell order does not exist")

        /** Have you transferred funds yet */
        if (doesSellOrderExists.orderStatus == OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS)
            return@coroutineScope DefaultResponse(message = "Order can not be cancelled because merchant  have already transferred funds")

        /** Update sell order state*/
        val updateSellOrder = doesSellOrderExists.copy(orderStatus = OrderStatus.CANCELLED)
        launch(Dispatchers.IO) { Entries.sellOrders.updateOne(BuyOrder::orderId eq sellOrderId, updateSellOrder) }

        /** Return funds from escrow to sellers crypto assets */
        val getSellersWalletFromEscrow = try {
            Entries.sellEscrowWallet.findOne(SellEscrowWallet::orderId eq sellOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred while getting sellers escrow wallet")

        val getSellersWallet = try {
            val result = Entries.userWallet.findOne(Wallet::walletId eq doesSellOrderExists.sellerEmail)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val updatedSellersAssets = mutableListOf<CryptoCoin>()
        getSellersWallet.assets.forEach {
            if(it.symbol == doesSellOrderExists.cryptoSymbol){
                updatedSellersAssets.add(
                    CryptoCoin(
                        symbol = it.symbol,
                        name = it.name,
                        amount = it.amount + doesSellOrderExists.cryptoAmount
                    )
                )
            }else{
                updatedSellersAssets.add(
                    CryptoCoin(
                        symbol = it.symbol,
                        name = it.name,
                        amount = it.amount
                    )
                )
            }
        }
        /** GetMerchants crypto ad*/
        val merchantsCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.sellAd.findOne(SellAd::id eq doesSellOrderExists.adId)
            }.await()
        } catch (e: Exception) {
            null
        } ?: return@coroutineScope DefaultResponse(message = "The crypto  sell Ad selected does not exist")

        /** Credit merchants cryptoAd*/
        val debitMerchantCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.sellAd
                    .updateOne(
                        SellAd::id eq doesSellOrderExists.adId,
                        merchantsCryptoAd.copy(totalAmount = merchantsCryptoAd.totalAmount + doesSellOrderExists.cryptoAmount)
                    )
                    .wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val creditSellersWallet = try {
            Entries.userWallet.updateOne(
                Wallet::walletId eq doesSellOrderExists.sellerEmail,
                getSellersWallet.copy(assets = updatedSellersAssets)
            ).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val deleteEscrowWallet = try {
            Entries.sellEscrowWallet.deleteOne(SellOrder::orderId eq sellOrderId).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val getMerchantData = try{
            Entries.sellAd.findOne(SellAd::id eq doesSellOrderExists.adId)
        }catch (e:Exception){
            e.printStackTrace()
            null
        }

        val notificationMessage = Notification(
            notificationType = NotificationType.SELL_ORDER_CANCELLED,
            notificationMessage = OrderCancellation(
                title = "Seller has cancelled the order",
                orderId = doesSellOrderExists.orderId,
                recipientEmail = getSellersWalletFromEscrow.merchantEmail,
                recipientName = getMerchantData?.merchantUsername ?: "",
                cryptoName = doesSellOrderExists.cryptoName,
                cryptoSymbol = doesSellOrderExists.cryptoSymbol,
                cryptoAmount = doesSellOrderExists.cryptoAmount,
                amountInKes = doesSellOrderExists.amountInKes
            )
        )
        launch(Dispatchers.IO) {
            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, message = gson.toJson(notificationMessage))
        }

        if (deleteEscrowWallet && creditSellersWallet) DefaultResponse(
            status = true,
            message = "Order has been cancelled"
        )
        else DefaultResponse(message = "An error occurred while cancelling the order")




    }

}

suspend fun main(){
    println(
        cancelSellOrder(sellOrderId = "654a1d59705b190dd70cec18")
    )
}
