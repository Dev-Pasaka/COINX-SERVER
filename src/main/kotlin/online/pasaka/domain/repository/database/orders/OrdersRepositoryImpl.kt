package online.pasaka.domain.repository.database.orders

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.model.order.BuyOrder
import online.pasaka.domain.model.order.OrderStatus
import online.pasaka.domain.model.order.SellOrder
import org.litote.kmongo.and
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.`in`


class OrdersRepositoryImpl(val entries: Entries = Entries) : OrdersRepository {
    override suspend fun getBuyOrders(email: String): List<BuyOrder>  = withContext(context = Dispatchers.IO){
            return@withContext async {
                try {
                    entries.cryptoBuyOrders.find(BuyOrder::buyersEmail eq email).toList()
                } catch (_: Exception) {
                    emptyList<BuyOrder>()
                }
            }.await()
    }

    override suspend fun getSellOrders(email: String): List<SellOrder> = withContext(context = Dispatchers.IO) {
            return@withContext async {
                try {
                    entries.sellOrders.find(SellOrder::sellerEmail eq email).toList()
                } catch (_: Exception) {
                    emptyList<SellOrder>()
                }
            }.await()
    }

    override suspend fun getBuyOrder(email: String): BuyOrder? = withContext(context = Dispatchers.IO) {
        return@withContext async{
            try {
                entries.cryptoBuyOrders.findOne(
                    BuyOrder::buyersEmail eq email,
                    BuyOrder::orderStatus eq OrderStatus.PENDING
                )
            }catch (e:Exception){
                null
            }
        }.await()
    }

    override suspend fun getSellOrder(email: String): SellOrder? = withContext(context = Dispatchers.IO) {
        return@withContext async{
            try {
                entries.sellOrders.findOne(
                    SellOrder::sellerEmail eq email,
                    SellOrder::orderStatus eq OrderStatus.PENDING,
                )
            }catch (e:Exception){
                null
            }
        }.await()
    }

}



