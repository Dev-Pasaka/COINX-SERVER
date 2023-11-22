package online.pasaka.domain.service.orders

import online.pasaka.domain.repository.database.orders.OrdersRepositoryImpl
import online.pasaka.domain.responses.ApiDefaultResponse

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
}