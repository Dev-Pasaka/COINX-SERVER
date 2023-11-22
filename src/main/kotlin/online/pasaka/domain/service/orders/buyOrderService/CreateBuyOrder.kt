package online.pasaka.domain.service.orders.buyOrderService

import com.google.gson.Gson
import kotlinx.coroutines.*
import online.pasaka.Kafka.models.*
import online.pasaka.Kafka.models.messages.BuyOrderConfirmationNotificationMessage
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.repository.remote.cryptodata.GetCryptoPrice
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.utils.Utils
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.updateOne

suspend fun createBuyOrder(buyOrder: online.pasaka.domain.model.order.BuyOrder): DefaultResponse {
    return coroutineScope {

/**Step 1: Retrieve the merchant's crypto ad for the selected buy order */
        val merchantsCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.buyAd.findOne(online.pasaka.domain.model.cryptoAds.BuyAd::id eq buyOrder.adId)
            }.await()
        } catch (e: Exception) {
            null
        } ?: return@coroutineScope DefaultResponse(message = "The crypto ad selected does not exist")

        /** Step 2: Check if the crypto ad has sufficient balance */
        if (merchantsCryptoAd.totalAmount < buyOrder.cryptoAmount) {
            return@coroutineScope DefaultResponse(message = "Crypto ad selected has insufficient balance. Choose another crypto ad")
        }

/** Step 3: Check if the crypto symbol in the order matches the crypto ad's symbol */
        val doesCryptoSymbolMatch = merchantsCryptoAd.cryptoSymbol == buyOrder.cryptoSymbol.uppercase()
        if (!doesCryptoSymbolMatch) {
            return@coroutineScope DefaultResponse(message = "The crypto selected does not match with the crypto ad")
        }

/** Step 4: Update the merchant's assets */
        val merchantAssets = merchantsCryptoAd.copy(totalAmount = merchantsCryptoAd.totalAmount - buyOrder.cryptoAmount)

/** Step 5: Generate a unique order ID */
        val orderId = ObjectId().toString()

/** Step 6: Fetch the current crypto price in USD */
        val cryptoPriceInKes = GetCryptoPrice().getCryptoMetadata(
            cryptoSymbol = buyOrder.cryptoSymbol.uppercase(),
            currency = "KES"
        ).price?.toDoubleOrNull()
            ?: return@coroutineScope DefaultResponse(message = "Failed to fetch current prices")

/** Calculate the amount to be transferred by the buyer */
        val transferAmountByBuyer = (buyOrder.cryptoAmount * cryptoPriceInKes) +
                (merchantsCryptoAd.margin * cryptoPriceInKes * buyOrder.cryptoAmount)

/** Step 7: Create an entry in the escrow wallet */
        val updateEscrowWallet = online.pasaka.domain.model.escrow.BuyEscrowWallet(
    orderId = orderId,
    merchantAdId = merchantsCryptoAd.id,
    merchantEmail = merchantsCryptoAd.email,
    buyerEmail = buyOrder.buyersEmail,
    cryptoName = merchantsCryptoAd.cryptoName,
    cryptoSymbol = buyOrder.cryptoSymbol,
    cryptoAmount = buyOrder.cryptoAmount,
    escrowState = online.pasaka.domain.model.escrow.EscrowState.PENDING,
    debitedAt = Utils.currentTimeStamp(),
    expiresAt = System.currentTimeMillis() + (60000 * 15)
)

/** Step 8: Debit the merchant's crypto ad */
        val debitCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.buyAd
                    .updateOne(online.pasaka.domain.model.cryptoAds.BuyAd::id eq buyOrder.adId, merchantAssets)
                    .wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

/** Step 9: Credit the escrow wallet */
        val creditEscrowWallet = try {
            async(Dispatchers.IO) {
                Entries.buyEscrowWallet
                    .insertOne(updateEscrowWallet)
                    .wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

/** Step 10: Create a buy order entry */
        val createOrder = online.pasaka.domain.model.order.BuyOrder(
    orderId = orderId,
    adId = merchantsCryptoAd.id,
    buyersEmail = merchantsCryptoAd.email,
    cryptoName = merchantsCryptoAd.cryptoName,
    cryptoSymbol = merchantsCryptoAd.cryptoSymbol,
    cryptoAmount = buyOrder.cryptoAmount,
    amountInKes = transferAmountByBuyer,
    orderStatus = online.pasaka.domain.model.order.OrderStatus.PENDING,
    expiresAt = buyOrder.expiresAt
)

/** Step 11: Insert the buy order into the database */
        val createBuyOrder = try {
            async(Dispatchers.IO) {
                Entries.cryptoBuyOrders.insertOne(createOrder).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        /** Get merchant's stats*/
        val getMerchantOrderStats = try {
            async(Dispatchers.IO) {
                Entries.dbMerchant.findOne(online.pasaka.domain.model.merchant.Merchant::email eq merchantsCryptoAd.email )
            }.await()
        }catch (e:Exception){
            e.printStackTrace()
            null
        }

        /** Update merchant's stats*/
        launch(Dispatchers.IO) {
            if (getMerchantOrderStats != null){
                val ordersCompletedByPercentage = (getMerchantOrderStats.ordersCompleted.toDouble()/getMerchantOrderStats.ordersMade.toDouble()) * 100
                Entries.dbMerchant.updateOne(
                    online.pasaka.domain.model.merchant.Merchant::email eq merchantsCryptoAd.email,
                    getMerchantOrderStats.copy(
                        ordersMade = getMerchantOrderStats.ordersMade + 1,
                        ordersCompletedByPercentage = ordersCompletedByPercentage.toDouble()
                    )

                )
            }
        }

/** Step 12: Send an email notification to the merchant */
        val gson = Gson()
        val notificationsMessage = BuyOrderConfirmationNotificationMessage(
            orderId = createOrder.orderId,
            title = "P2P Order Confirmation",
            iconUrl = "https://play-lh.googleusercontent.com/Yg7Lo7wiW-iLzcnaarj7nm5-hQjl7J9eTgEupxKzC79Vq8qyRgTBnxeWDap-yC8kHoE=w240-h480-rw",
            recipientName = merchantsCryptoAd.merchantUsername,
            recipientEmail = merchantsCryptoAd.email,
            cryptoName = createOrder.cryptoName,
            cryptoSymbol = createOrder.cryptoSymbol,
            cryptoAmount = createOrder.cryptoAmount,
            amountInKes = createOrder.amountInKes
        )
        val emailNotificationMessage = Notification(
            notificationType = NotificationType.BUY_ORDER_HAS_BEEN_PLACED,
            notificationMessage = notificationsMessage
        )
        launch(Dispatchers.IO) {
            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, message = gson.toJson(emailNotificationMessage))
        }

        /** Step 13: Await asynchronous operations */
        createBuyOrder?.await() ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")
        val debitCryptoAdResult = debitCryptoAd?.await() ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")
        val creditEscrowWalletResult = creditEscrowWallet?.await() ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        /** Step 14: Check results and return the appropriate response */
        if (debitCryptoAdResult && creditEscrowWalletResult) {
            return@coroutineScope DefaultResponse(
                status = true,
                message = "Merchant's assets are in holding in escrow"
            )
        } else {
            return@coroutineScope DefaultResponse(message = "An expected error has occurred")
        }
    }
}



