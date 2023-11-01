package online.pasaka.service.buyOrderService

import com.google.gson.Gson
import kotlinx.coroutines.*
import online.pasaka.Kafka.models.*
import online.pasaka.Kafka.models.messages.BuyOrderConfirmationNotificationMessage
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.database.DatabaseConnection
import online.pasaka.database.Entries
import online.pasaka.model.cryptoAds.CreateCryptoBuyAd
import online.pasaka.model.escrow.BuyEscrowWallet
import online.pasaka.model.escrow.EscrowState
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.repository.cryptodata.GetCryptoPrice
import online.pasaka.responses.DefaultResponse
import online.pasaka.utils.Utils
import org.bson.types.ObjectId
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOne

suspend fun createBuyOrder(
    buyOrder: BuyOrder
): DefaultResponse {

    return coroutineScope {

        val merchantsCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.merchantCryptoBuyAd.findOne(CreateCryptoBuyAd::id eq buyOrder.adId)
            }.await()
        } catch (e: Exception) {
            null
        } ?: return@coroutineScope DefaultResponse(message = "The crypto add selected does not exit")

        if (merchantsCryptoAd.totalAmount < buyOrder.cryptoAmount)
            return@coroutineScope DefaultResponse(message = "Crypto ad selected has insufficient balance kindly choose another  crypto AD")

        val doesCryptoSymbolMatch = merchantsCryptoAd.cryptoSymbol == buyOrder.cryptoSymbol.uppercase()
        if (!doesCryptoSymbolMatch) return@coroutineScope DefaultResponse(message = "The crypto selected don't match with the crypto ad")

        val merchantAssets =
            merchantsCryptoAd.copy(totalAmount = merchantsCryptoAd.totalAmount - buyOrder.cryptoAmount)

        val orderId = ObjectId().toString()
        val cryptoPriceInUSD = GetCryptoPrice().getCryptoMetadata(cryptoSymbol = buyOrder.cryptoSymbol.uppercase(), currency = "KES").price?.toDoubleOrNull()
                ?: return@coroutineScope DefaultResponse(message = "Failed to fetch current prices")
        val transferAmountByBuyer =
            (buyOrder.cryptoAmount * cryptoPriceInUSD) + (merchantsCryptoAd.margin * (cryptoPriceInUSD)*buyOrder.cryptoAmount)

        val updateEscrowWallet = BuyEscrowWallet(
            orderId = orderId,
            merchantAdId = merchantsCryptoAd.id,
            merchantEmail = merchantsCryptoAd.email,
            buyerEmail = buyOrder.buyersEmail,
            cryptoName = merchantsCryptoAd.cryptoName,
            cryptoSymbol = buyOrder.cryptoSymbol,
            cryptoAmount = buyOrder.cryptoAmount,
            escrowState = EscrowState.PENDING,
            debitedAt = Utils.currentTimeStamp(),
            expiresAt = System.currentTimeMillis() + (60000*1)
        )

        val debitCryptoAd = try {
            async(Dispatchers.IO) {
                Entries.merchantCryptoBuyAd
                    .updateOne(CreateCryptoBuyAd::id eq buyOrder.adId, merchantAssets)
                    .wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

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

        val createOrder = BuyOrder(
            orderId = orderId,
            adId = merchantsCryptoAd.id,
            buyersEmail = merchantsCryptoAd.email,
            cryptoName = merchantsCryptoAd.cryptoName,
            cryptoSymbol = buyOrder.cryptoSymbol,
            cryptoAmount = buyOrder.cryptoAmount,
            amountInKes = transferAmountByBuyer,
            orderStatus = OrderStatus.PENDING,
            expiresAt = buyOrder.expiresAt
        )
        val createBuyOrder = try {
            async(Dispatchers.IO) {
                Entries.cryptoBuyOrders.insertOne(createOrder).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

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
            notificationType = NotificationType.ORDER_HAS_BEEN_PLACED,
            notificationMessage = notificationsMessage
        )
        launch(Dispatchers.IO) {
            kafkaProducer(topic = KafkaConfig.EMAIL_NOTIFICATIONS, message = gson.toJson(emailNotificationMessage))
        }

        createBuyOrder?.await()
            ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")
        val debitCryptoAdResult = debitCryptoAd?.await()
            ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")
        val creditEscrowWalletResult = creditEscrowWallet?.await()
            ?: return@coroutineScope DefaultResponse(message = "An expected error has occurred")

        if (debitCryptoAdResult && creditEscrowWalletResult)
        {
            delay(60000)
            return@coroutineScope DefaultResponse(
                status = true,
                message = "Merchants assets are in holding in escrow"
            )
        }
        else DefaultResponse(message = "An expected has occurred")


    }


}

suspend fun main() {
    println(
        createBuyOrder(
            buyOrder = BuyOrder(
                orderId = "8976534",
                adId = "6536eac4786c8d3f2151f8ed",
                buyersEmail = "dev.pasaka@gmail.com",
                cryptoName = "Tether",
                cryptoSymbol = "ADA",
                cryptoAmount = 40.0,
                amountInKes = 7400.0,
                expiresAt = System.currentTimeMillis() + (60000*15).toLong()
            )
        )
    )
}
