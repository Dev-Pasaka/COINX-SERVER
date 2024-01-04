package online.pasaka.domain.service.orders

import online.pasaka.domain.model.order.BuyOrder
import online.pasaka.domain.model.order.SellOrder
import online.pasaka.domain.repository.database.orders.OrdersRepositoryImpl
import online.pasaka.domain.responses.ApiDefaultResponse
import online.pasaka.domain.responses.GetBuyOrderResponse
import online.pasaka.domain.responses.GetSellOrderResponse

object Orders {
    suspend fun getBuyOrders(email:String):ApiDefaultResponse{
        val result = OrdersRepositoryImpl().getBuyOrders(email = email)
        return when(result.isNotEmpty()){
            true -> ApiDefaultResponse(status = true, message = result)
            false -> ApiDefaultResponse(status = false, message = result)
        }
    }

    suspend fun getSellOrders(email:String):ApiDefaultResponse{
        val result = OrdersRepositoryImpl().getSellOrders(email = email)
        return when(result.isNotEmpty()){
            true -> ApiDefaultResponse(status = true, message = result)
            false -> ApiDefaultResponse(status = false, message = result)
        }
    }

    suspend fun getBuyOrder(email:String):GetBuyOrderResponse {
        val result =  OrdersRepositoryImpl().getBuyOrder(email = email)
        return when(result != null){
            true -> GetBuyOrderResponse(status = true, message = result)
            false -> GetBuyOrderResponse(status = false, message = result)
        }
    }
    suspend fun getSellOrder(email:String): GetSellOrderResponse{
      val result = OrdersRepositoryImpl().getSellOrder(email = email)
      return when(result != null){
          true -> GetSellOrderResponse(status = true, message = result)
          false -> GetSellOrderResponse(status = false, message = result)
      }
    }


}