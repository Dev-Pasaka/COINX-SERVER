package online.pasaka.domain.service.orders.sellOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.OrderCancellation
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.updateOne

suspend fun cancelSellOrder(sellOrderId: String): DefaultResponse {
    val gson = Gson()

    return coroutineScope {

        /** Does order exits */
        val doesSellOrderExists = try {
            Entries.sellOrders.findOne(online.pasaka.domain.model.order.SellOrder::orderId eq sellOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto sell order does not exist")

        /** Have you transferred funds yet */
        if (doesSellOrderExists.orderStatus == online.pasaka.domain.model.order.OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS)
            return@coroutineScope DefaultResponse(message = "Order can not be cancelled because merchant  have already transferred funds")

        /** Update sell order state*/
        val updateSellOrder = doesSellOrderExists.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.CANCELLED)
        launch(Dispatchers.IO) { Entries.sellOrders.updateOne(online.pasaka.domain.model.order.BuyOrder::orderId eq sellOrderId, updateSellOrder) }

        /** Return funds from escrow to sellers crypto assets */
        val getSellersWalletFromEscrow = try {
            Entries.sellEscrowWallet.findOne(online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq sellOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred while getting sellers escrow wallet")

        val getSellersWallet = try {
            val result = Entries.userWallet.findOne(online.pasaka.domain.model.wallet.Wallet::walletId eq doesSellOrderExists.sellerEmail)
            result
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val updatedSellersAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
        getSellersWallet.assets.forEach {
            if(it.symbol == doesSellOrderExists.cryptoSymbol){
                updatedSellersAssets.add(
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                        symbol = it.symbol,
                        name = it.name,
                        amount = it.amount + doesSellOrderExists.cryptoAmount
                    )
                )
            }else{
                updatedSellersAssets.add(
                    online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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
                Entries.sellAd.findOne(online.pasaka.domain.model.cryptoAds.SellAd::id eq doesSellOrderExists.adId)
            }.await()
        } catch (e: Exception) {
            null
        } ?: return@coroutineScope DefaultResponse(message = "The crypto  sell Ad selected does not exist")

        /** Credit merchants cryptoAd*/
        val debitMerchantCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.sellAd
                    .updateOne(
                        online.pasaka.domain.model.cryptoAds.SellAd::id eq doesSellOrderExists.adId,
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
                online.pasaka.domain.model.wallet.Wallet::walletId eq doesSellOrderExists.sellerEmail,
                getSellersWallet.copy(assets = updatedSellersAssets)
            ).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val deleteEscrowWallet = try {
            Entries.sellEscrowWallet.deleteOne(online.pasaka.domain.model.order.SellOrder::orderId eq sellOrderId).wasAcknowledged()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        val getMerchantData = try{
            Entries.sellAd.findOne(online.pasaka.domain.model.cryptoAds.SellAd::id eq doesSellOrderExists.adId)
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


