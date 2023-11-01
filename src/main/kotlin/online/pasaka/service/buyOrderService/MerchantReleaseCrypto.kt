package online.pasaka.service.buyOrderService

import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.messages.MerchantReleaseCryptoAssets
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.database.DatabaseConnection
import online.pasaka.database.Entries
import online.pasaka.model.cryptoAds.CreateCryptoBuyAd
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.model.wallet.Wallet
import online.pasaka.model.wallet.crypto.CryptoCoin
import online.pasaka.responses.DefaultResponse
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne


suspend fun merchantReleaseCrypto(buyOrderID: String): DefaultResponse {
    val gson = Gson()


    return coroutineScope {

        /** Get order status*/
        val orderStatus = try {
            Entries.cryptoBuyOrders.findOne(BuyOrder::orderId eq buyOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
            ?: return@coroutineScope DefaultResponse("Order not found kindly check with customer service for more information")
        when (orderStatus.orderStatus) {
            OrderStatus.CANCELLED -> return@coroutineScope DefaultResponse("This order is already cancelled")
            OrderStatus.EXPIRED -> return@coroutineScope DefaultResponse("This order has expired kindly check with customer service for more information")
            OrderStatus.COMPLETED -> return@coroutineScope DefaultResponse("This Order is already completed. kindly check with customer service for more information")
            else -> {}
        }

        /** Get escrow wallet data */
        val getEscrowWalletData = try {
            Entries.buyEscrowWallet.findOne(BuyEscrowWallet::orderId eq buyOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist in escrow wallet")

        /** Get buyers wallet and update cryptoAmount*/
        val getBuyersWalletAssets = try {
            Entries.buyersWallet.findOne(Wallet::walletId eq getEscrowWalletData.buyerEmail)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse("An expected error has occurred")

        println("previous buyers assets: ${getBuyersWalletAssets.assets}")

        val updateBuyersAssets = mutableListOf<CryptoCoin>()
        getBuyersWalletAssets.assets.forEach {
            if (it.symbol == getEscrowWalletData.cryptoSymbol) {
                val updatedAmount = it.amount + getEscrowWalletData.cryptoAmount
                val updatedCryptoCoin = CryptoCoin(
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
            val escrowCryptoCoin = CryptoCoin(
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
                    BuyEscrowWallet::buyerEmail eq getEscrowWalletData.buyerEmail,
                    getEscrowWalletData.copy(cryptoAmount = 0.0)
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
                    Wallet::walletId eq getEscrowWalletData.buyerEmail,
                    getBuyersWalletAssets.copy(assets = updateBuyersAssets)
                ).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
        val debitEscrowWalletResult = debitEscrowWallet?.await()
        val creditAmountToBuyersWalletResult = creditAmountToBuyersWallet?.await()

        if (debitEscrowWalletResult == true && creditAmountToBuyersWalletResult == true) {

            println("News users assets: $updateBuyersAssets")
            /** Notify the buyer that crypto has been deposited to his wallet */
            val notification = Notification(
                notificationType = NotificationType.COMPLETED,
                notificationMessage = MerchantReleaseCryptoAssets(
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
                    Entries.cryptoBuyOrders.findOne(BuyOrder::orderId eq getEscrowWalletData.orderId)
                }.await()?.copy(orderStatus = OrderStatus.COMPLETED)
                if (updateOrderStatus != null) {
                    Entries.cryptoBuyOrders.updateOne(BuyOrder::orderId eq getEscrowWalletData.orderId, updateOrderStatus)
                        .wasAcknowledged()
                }
            }
            return@coroutineScope DefaultResponse(status = true, message = "Crypto assets have been released to buyer")

        } else {
            merchantReleaseCrypto(buyOrderID = buyOrderID)
        }


    }
}

suspend fun main() {
    println(
        merchantReleaseCrypto(buyOrderID = "653b89a4935cfd4d8e5a0058")
    )
}