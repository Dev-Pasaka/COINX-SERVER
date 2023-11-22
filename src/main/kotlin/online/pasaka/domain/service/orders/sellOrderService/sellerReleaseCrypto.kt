package online.pasaka.domain.service.orders.sellOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.models.messages.ReleaseCrypto
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.updateOne

suspend fun sellerReleaseCrypto(sellOrderId: String): DefaultResponse {
    val gson = Gson()
    return coroutineScope {

        /** Get order status */
        val orderStatus = try {
            Entries.sellOrders.findOne(online.pasaka.domain.model.order.SellOrder::orderId eq sellOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse("Order not found kindly check with customer service for more information")

        when (orderStatus.orderStatus) {
            online.pasaka.domain.model.order.OrderStatus.PENDING -> return@coroutineScope DefaultResponse( "This Order is in processing kindly wait for merchant to transfer funds before releasing.")
            online.pasaka.domain.model.order.OrderStatus.CANCELLED -> return@coroutineScope DefaultResponse("This order is already cancelled")
            online.pasaka.domain.model.order.OrderStatus.EXPIRED -> return@coroutineScope DefaultResponse("This order has expired kindly check with customer service for more information")
            online.pasaka.domain.model.order.OrderStatus.COMPLETED -> return@coroutineScope DefaultResponse("This Order is already completed. kindly check with customer service for more information")
            else -> {  }
        }

        /** Get escrow wallet data */
        val getEscrowWalletData = try {
            Entries.sellEscrowWallet.findOne(online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq sellOrderId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto sell order does not exist in escrow wallet")

        /** Get merchant's wallet and update cryptoAmount*/
        val getMerchantsWalletAssets = try {
            Entries.dbMerchantWallet.findOne(online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq getEscrowWalletData.merchantEmail)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse("An expected error has occurred")

        println("previous buyers assets: ${getMerchantsWalletAssets.assets}")

        val updatedMerchantsAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
        getMerchantsWalletAssets.assets.forEach {
            if (it.symbol == getEscrowWalletData.cryptoSymbol) {
                val updatedAmount = it.amount + getEscrowWalletData.cryptoAmount
                val updatedCryptoCoin = online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                    symbol = it.symbol,
                    amount = updatedAmount,
                    name = it.name
                )
                updatedMerchantsAssets.add(updatedCryptoCoin)
            } else {
                updatedMerchantsAssets.add(it)
            }
        }


        /** Now add the escrow wallet data if it doesn't exist in the original assets*/
        val escrowAsset = updatedMerchantsAssets.find { it.symbol == getEscrowWalletData.cryptoSymbol }
        if (escrowAsset == null) {
            val escrowCryptoCoin = online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                symbol = getEscrowWalletData.cryptoSymbol,
                amount = getEscrowWalletData.cryptoAmount,
                name = getEscrowWalletData.cryptoName
            )
            updatedMerchantsAssets.add(escrowCryptoCoin)
            updatedMerchantsAssets.addAll(getMerchantsWalletAssets.assets)
        }


        /** Debit amount from escrow wallet */
        val debitEscrowWallet = try {
            async(Dispatchers.IO) {
                Entries.sellEscrowWallet.updateOne(
                    online.pasaka.domain.model.escrow.SellEscrowWallet::orderId eq getEscrowWalletData.orderId,
                    getEscrowWalletData.copy(cryptoAmount = 0.0, escrowState = online.pasaka.domain.model.escrow.EscrowState.CRYPTO_RELEASED_TO_MERCHANT)
                ).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        /** Credit amount to Merchants Wallet */
        val creditAmountToBuyersWallet = try {
            async(Dispatchers.IO) {
                Entries.dbMerchantWallet.updateOne(
                    online.pasaka.domain.model.merchant.wallet.MerchantWallet::walletId eq getEscrowWalletData.sellersEmail,
                    getMerchantsWalletAssets.copy(assets = updatedMerchantsAssets)
                ).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        val getMerchantOrderStats = try {
            async(Dispatchers.IO) {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq getEscrowWalletData.merchantEmail )
            }.await()
        }catch (e:Exception){
            e.printStackTrace()
            null
        }

        /** Update merchant's stats*/
        launch(Dispatchers.IO) {
            if (getMerchantOrderStats != null){
                Entries.dbMerchant.updateOne(
                    online.pasaka.domain.model.merchant.Merchant::email eq getEscrowWalletData.merchantEmail,
                    getMerchantOrderStats.copy(ordersCompleted = getMerchantOrderStats.ordersCompleted + 1)

                )
            }
        }

        val debitEscrowWalletResult = debitEscrowWallet?.await()
        val creditAmountToBuyersWalletResult = creditAmountToBuyersWallet?.await()

        if (debitEscrowWalletResult == true && creditAmountToBuyersWalletResult == true) {

            println("News users assets: $updatedMerchantsAssets")

            /** Notify the merchant that crypto has been deposited to his wallet */
            val notification = Notification(
                notificationType = NotificationType.SELL_ORDER_COMPLETED,
                notificationMessage = ReleaseCrypto(
                    title = "Deposit Was Successful",
                    orderId = getEscrowWalletData.orderId,
                    recipientEmail = getEscrowWalletData.merchantEmail,
                    cryptoName = getEscrowWalletData.cryptoName,
                    cryptoSymbol = getEscrowWalletData.cryptoSymbol,
                    cryptoAmount = getEscrowWalletData.cryptoAmount,
                )
            )
            launch(Dispatchers.IO) {
                kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, gson.toJson(notification))
            }
            launch(Dispatchers.IO) {
                val updateOrderStatus = async {
                    Entries.sellOrders.findOne(online.pasaka.domain.model.order.SellOrder::orderId eq getEscrowWalletData.orderId)
                }.await()?.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.COMPLETED)
                if (updateOrderStatus != null) {
                    Entries.sellOrders.updateOne(online.pasaka.domain.model.order.SellOrder::orderId eq getEscrowWalletData.orderId, updateOrderStatus)
                        .wasAcknowledged()
                }
            }
            return@coroutineScope DefaultResponse(status = true, message = "Crypto assets have been released to buyer")

        } else {
            sellerReleaseCrypto(sellOrderId = sellOrderId)
        }


    }
}

