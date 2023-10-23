package online.pasaka.service.buyOrderService

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.database.DatabaseConnection
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

    val buyEscrowWallet = DatabaseConnection.database.getCollection<BuyEscrowWallet>("EscrowWallet")
    val cryptoBuyOrders = DatabaseConnection.database.getCollection<BuyOrder>("BuyOrders")
    val buyersWallet = DatabaseConnection.database.getCollection<Wallet>()

    return coroutineScope {

        /** Get escrow wallet data */
        /** Get escrow wallet data */
        val getEscrowWalletData = try {
            buyEscrowWallet.findOne(BuyEscrowWallet::orderId eq buyOrderID)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse(message = "Crypto buy order does not exist in escrow wallet")

        /** Get buyers wallet and update cryptoAmount*/
        /** Get buyers wallet and update cryptoAmount*/
        val getBuyersWalletAssets = try {
            buyersWallet.findOne(Wallet::walletId eq getEscrowWalletData.buyerEmail)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } ?: return@coroutineScope DefaultResponse("An expected error has occurred")

        val updateBuyersAssets = mutableListOf<CryptoCoin>()
        getBuyersWalletAssets.assets.forEach {
            if (it.symbol == getEscrowWalletData.cryptoSymbol) {
                updateBuyersAssets.add(
                    CryptoCoin(
                        symbol = it.symbol,
                        amount = it.amount + getEscrowWalletData.cryptoAmount,
                        name = it.name
                    )
                )
                println("New assets: $it")
            } else {
                updateBuyersAssets.add(
                    CryptoCoin(
                        symbol = it.symbol,
                        amount = it.amount,
                        name = it.name
                    )
                )
            }
        }

        /** Debit amount from escrow wallet */
        /** Debit amount from escrow wallet */
        val debitEscrowWallet = try {
            async {
                buyEscrowWallet.updateOne(
                    BuyEscrowWallet::buyerEmail eq getEscrowWalletData.buyerEmail,
                    getEscrowWalletData.copy(cryptoAmount = 0.0)
                ).wasAcknowledged()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }

        /** Credit amount to Buyers Wallet */
        /** Credit amount to Buyers Wallet */
        val creditAmountToBuyersWallet = try {
            async {
                buyersWallet.updateOne(
                    Wallet::walletId eq getEscrowWalletData.buyerEmail,
                    getBuyersWalletAssets.copy(assets = updateBuyersAssets)
                ).wasAcknowledged()
            }
        }catch (e:Exception){
            e.printStackTrace()
            null
        }
        val debitEscrowWalletResult = debitEscrowWallet?.await()
        val creditAmountToBuyersWalletResult = creditAmountToBuyersWallet?.await()

        if (debitEscrowWalletResult == true && creditAmountToBuyersWalletResult == true){
            /** Notify the buyer that crypto has been deposited to his wallet */
            /** Notify the buyer that crypto has been deposited to his wallet */

            launch {
                val updateOrderStatus = async {
                    cryptoBuyOrders.findOne(BuyOrder::orderId eq getEscrowWalletData.orderId)
                }.await()?.copy(orderStatus = OrderStatus.COMPLETED)
                if (updateOrderStatus != null) {
                    cryptoBuyOrders.updateOne(BuyOrder::orderId eq getEscrowWalletData.orderId, updateOrderStatus).wasAcknowledged()
                }
            }
            return@coroutineScope DefaultResponse( status = true, message = "Crypto assets have been released to buyer")

        }else{
            TODO("Retry the transaction")
        }

        /** Update Order state */

        /** Update Order state */





    }
}