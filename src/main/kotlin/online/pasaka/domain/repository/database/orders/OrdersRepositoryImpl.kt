package online.pasaka.domain.repository.database.orders

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.infrastructure.database.Entries
import online.pasaka.domain.model.order.BuyOrder
import online.pasaka.domain.model.order.SellOrder
import org.litote.kmongo.eq


class OrdersRepositoryImpl(val entries: Entries = Entries) : OrdersRepository {
    override suspend fun getBuyOrders(email: String): List<BuyOrder> {
        return coroutineScope {
            return@coroutineScope async {
                try {
                    entries.cryptoBuyOrders.find(BuyOrder::buyersEmail eq email).toList()
                } catch (_: Exception) {
                    emptyList<BuyOrder>()
                }
            }.await()
        }
    }

    override suspend fun getSellOrders(email: String): List<SellOrder> {
        return coroutineScope {
            return@coroutineScope async {
                try {
                    entries.sellOrders.find(SellOrder::sellerEmail eq email).toList()
                } catch (_: Exception) {
                    emptyList<SellOrder>()
                }
            }.await()
        }
    }

}

