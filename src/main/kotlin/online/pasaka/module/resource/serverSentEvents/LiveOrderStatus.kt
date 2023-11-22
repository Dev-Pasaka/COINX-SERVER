package online.pasaka.module.resource.serverSentEvents

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import online.pasaka.infrastructure.database.Entries
import org.litote.kmongo.*

fun Route.liveOrderStatus() {
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
                                            online.pasaka.domain.model.order.BuyOrder::orderStatus `in` listOf(
                                                online.pasaka.domain.model.order.OrderStatus.PENDING,
                                                online.pasaka.domain.model.order.OrderStatus.BUYER_HAS_TRANSFERRED_FUNDS,
                                                online.pasaka.domain.model.order.OrderStatus.COMPLETED,
                                            ),
                                            online.pasaka.domain.model.order.BuyOrder::expiresAt gt System.currentTimeMillis() + 60000,

                                            ),
                                        online.pasaka.domain.model.order.BuyOrder::buyersEmail eq email
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
                                            online.pasaka.domain.model.order.SellOrder::orderStatus `in` listOf(
                                                online.pasaka.domain.model.order.OrderStatus.PENDING,
                                                online.pasaka.domain.model.order.OrderStatus.MERCHANT_HAS_TRANSFERRED_FUNDS,
                                                online.pasaka.domain.model.order.OrderStatus.COMPLETED,
                                            ),
                                            online.pasaka.domain.model.order.SellOrder::expiresAt gt System.currentTimeMillis(),

                                            ),
                                        online.pasaka.domain.model.order.SellOrder::sellerEmail eq email
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


