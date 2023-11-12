package online.pasaka.resource.serverSentEvents

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import online.pasaka.database.Entries
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.model.order.SellOrder
import org.litote.kmongo.*

fun Route.sentEvents() {
    val gson = Gson()
    authenticate("auth-jwt") {
        get("/liveOrders") {
            coroutineScope {
                launch {
                    val email = call.principal<JWTPrincipal>()
                        ?.payload
                        ?.getClaim("email")
                        .toString()
                        .removeSurrounding("\"")

                    call.respondTextWriter(contentType = ContentType.Text.EventStream) {

                        while (true) {
                            val buyersOrders = try {
                                async {
                                    Entries.cryptoBuyOrders.find(
                                        and(
                                            BuyOrder::orderStatus `in` listOf(
                                                OrderStatus.PENDING,
                                                OrderStatus.BUYER_HAS_TRANSFERRED_FUNDS,
                                                OrderStatus.COMPLETED,
                                            ),
                                            BuyOrder::expiresAt gt System.currentTimeMillis() + 60000,

                                            ),
                                        BuyOrder::buyersEmail eq email
                                    ).toList()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                            val sellOrders = try {
                                async {
                                    Entries.sellOrders.find(
                                        and(
                                            SellOrder::orderStatus `in` listOf(
                                                OrderStatus.PENDING,
                                                OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS,
                                                OrderStatus.COMPLETED,
                                            ),
                                            SellOrder::expiresAt gt System.currentTimeMillis(),

                                            ),
                                        SellOrder::sellerEmail eq email
                                    ).toList()
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                                null
                            }
                            val buyOrdersResult = buyersOrders?.await()
                            val sellOrdersResult = sellOrders?.await()

                            val result = mutableListOf<Map<String, Any>>()
                            val isBuyOrderResultEmpty = buyOrdersResult?.isNotEmpty()
                            buyOrdersResult?.forEach {
                                if (isBuyOrderResultEmpty == true){
                                    result.add(
                                        mapOf("OrderType" to "BuyOrder","orderId" to it.orderId,   "orderStatus" to  it.orderStatus, "status" to isBuyOrderResultEmpty )
                                    )
                                }
                            }
                            val isSellOrderResultEmpty = sellOrdersResult?.isNotEmpty()
                            sellOrdersResult?.forEach {
                                if (isSellOrderResultEmpty == true){
                                    result.add(
                                        mapOf("OrderType" to "SellOrder","orderId" to it.orderId,   "orderStatus" to  it.orderStatus, "status" to isSellOrderResultEmpty)
                                    )
                                }
                            }
                            val event = gson.toJson(result)
                            write("data: $event\n\n")
                            flush()
                            delay(10000)
                        }
                    }
                }
            }


        }
    }
}


suspend fun main(){
}
