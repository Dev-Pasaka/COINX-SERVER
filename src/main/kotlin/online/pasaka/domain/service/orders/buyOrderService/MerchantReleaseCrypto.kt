package online.pasaka.domain.service.orders.buyOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.messages.ReleaseCrypto
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.updateOne


suspend fun merchantReleaseCrypto(buyOrderID: String): DefaultResponse {
    val gson = Gson()


    return coroutineScope {

        /** Get order status*/
        val orderStatus = try {
            Entries.cryptoBuyOrders.findOne(online.pasaka.domain.model.order.BuyOrder::orderId eq buyOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
            ?: return@coroutineScope DefaultResponse("Order not found kindly check with customer service for more information")
        when (orderStatus.orderStatus) {
            online.pasaka.domain.model.order.OrderStatus.CANCELLED -> return@coroutineScope DefaultResponse("This order is already cancelled")
            online.pasaka.domain.model.order.OrderStatus.EXPIRED -> return@coroutineScope DefaultResponse("This order has expired kindly check with customer service for more information")
            online.pasaka.domain.model.order.OrderStatus.PENDING -> return@coroutineScope DefaultResponse( "This Order is in processing kindly wait for buyer to transfer funds before releasing.")
            online.pasaka.domain.model.order.OrderStatus.COMPLETED -> return@coroutineScope DefaultResponse("This Order is already completed. kindly check with customer service for more information")
            else -> {}
        }

        /** Get escrow wallet data */
        val getEscrowWalletData = try {
            Entries.buyEscrowWallet.findOne(online.pasaka.domain.model.escrow.BuyEscrowWallet::orderId eq buyOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist in escrow wallet")

        /** Get buyers wallet and update cryptoAmount*/
        val getBuyersWalletAssets = try {
            Entries.buyersWallet.findOne(online.pasaka.domain.model.wallet.Wallet::walletId eq getEscrowWalletData.buyerEmail)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse("An expected error has occurred")

        println("previous buyers assets: ${getBuyersWalletAssets.assets}")

        val updateBuyersAssets = mutableListOf<online.pasaka.domain.model.wallet.crypto.CryptoCoin>()
        getBuyersWalletAssets.assets.forEach {
            if (it.symbol == getEscrowWalletData.cryptoSymbol) {
                val updatedAmount = it.amount + getEscrowWalletData.cryptoAmount
                val updatedCryptoCoin = online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                    symbol = it.symbol,
                    amount = updatedAmount,
                    name = it.name
                )
                updateBuyersAssets.add(updatedCryptoCoin)
            } else {
                updateBuyersAssets.add(it)
            }
        }


        /**Now add the escrow wallet data if it doesn't exist in the original assets*/
        val escrowAsset = updateBuyersAssets.find { it.symbol == getEscrowWalletData.cryptoSymbol }
        if (escrowAsset == null) {
            val escrowCryptoCoin = online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                symbol = getEscrowWalletData.cryptoSymbol,
                amount = getEscrowWalletData.cryptoAmount,
                name = getEscrowWalletData.cryptoName
            )
            updateBuyersAssets.add(escrowCryptoCoin)
            updateBuyersAssets.addAll(getBuyersWalletAssets.assets)
        }


        /** Debit amount from escrow wallet */
        val debitEscrowWallet = try {
            async(Dispatchers.IO) {
                Entries.buyEscrowWallet.updateOne(
                    online.pasaka.domain.model.escrow.BuyEscrowWallet::buyerEmail eq getEscrowWalletData.buyerEmail,
                    getEscrowWalletData.copy(cryptoAmount = 0.0, escrowState = online.pasaka.domain.model.escrow.EscrowState.CRYPTO_RELEASED_TO_BUYER)
                ).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        /** Credit amount to Buyers Wallet */
        val creditAmountToBuyersWallet = try {
            async(Dispatchers.IO) {
                Entries.buyersWallet.updateOne(
                    online.pasaka.domain.model.wallet.Wallet::walletId eq getEscrowWalletData.buyerEmail,
                    getBuyersWalletAssets.copy(assets = updateBuyersAssets)
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
                    getMerchantOrderStats.copy(
                        ordersCompleted = getMerchantOrderStats.ordersCompleted + 1,
                    )


                )
            }
        }



        val debitEscrowWalletResult = debitEscrowWallet?.await()
        val creditAmountToBuyersWalletResult = creditAmountToBuyersWallet?.await()

        if (debitEscrowWalletResult == true && creditAmountToBuyersWalletResult == true) {

            println("News users assets: $updateBuyersAssets")
            /** Notify the buyer that crypto has been deposited to his wallet */
            val notification = Notification(
                notificationType = NotificationType.BUY_ORDER_COMPLETED,
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
                    Entries.cryptoBuyOrders.findOne(online.pasaka.domain.model.order.BuyOrder::orderId eq getEscrowWalletData.orderId)
                }.await()?.copy(orderStatus = online.pasaka.domain.model.order.OrderStatus.COMPLETED)
                if (updateOrderStatus != null) {
                    Entries.cryptoBuyOrders.updateOne(online.pasaka.domain.model.order.BuyOrder::orderId eq getEscrowWalletData.orderId, updateOrderStatus)
                        .wasAcknowledged()
                }
            }
            return@coroutineScope DefaultResponse(status = true, message = "Crypto assets have been released to buyer")

        } else {
            merchantReleaseCrypto(buyOrderID = buyOrderID)
        }


    }
}

