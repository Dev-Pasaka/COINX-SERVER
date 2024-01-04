package online.pasaka.domain.repository.database.orders

import online.pasaka.domain.model.order.BuyOrder
import online.pasaka.domain.model.order.SellOrder

interface OrdersRepository{

    suspend fun getBuyOrders(email:String):List<BuyOrder>
    suspend fun getSellOrders(email:String):List<SellOrder>
    suspend fun getBuyOrder(email: String):BuyOrder?
    suspend fun getSellOrder(email: String):SellOrder?

}