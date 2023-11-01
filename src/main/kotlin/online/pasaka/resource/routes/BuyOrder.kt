package online.pasaka.resource.routes

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.messages.BuyOrderMessage
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.dto.BuyOrderDto
import online.pasaka.responses.DefaultResponse
import online.pasaka.service.buyOrderService.buyerHasTransferredFundsToMerchant
import online.pasaka.service.buyOrderService.cancelOrder
import online.pasaka.service.buyOrderService.merchantReleaseCrypto


fun Route.cryptoBuyOrder() {
    authenticate("auth-jwt") {

        post("/cryptoBuyOrder") {

            coroutineScope {

                val email = call.principal<JWTPrincipal>()
                    ?.payload
                    ?.getClaim("email")
                    .toString()
                    .removeSurrounding("\"")

                val buyOrder = call.receive<BuyOrderDto>()
                val buyOrderMessage = BuyOrderMessage(
                    adId = buyOrder.adId,
                    buyersEmail = email,
                    cryptoName = buyOrder.cryptoName,
                    cryptoSymbol = buyOrder.cryptoSymbol,
                    cryptoAmount = buyOrder.cryptoAmount,
                )
                val gson = Gson()
                val buyOrderJsonString = gson.toJson(buyOrderMessage)

                try {
                    launch { kafkaProducer(topic = KafkaConfig.CRYPTO_BUY_ORDERS, message = buyOrderJsonString) }

                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error occurred when creating your order. Please try again later"
                    )
                )


                call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = false,
                        message = "Your buy order has been placed successfully "
                    )
                )


            }


        }

    }

}
fun Route.buyerTransferredFunds() {

    authenticate("auth-jwt") {
        get("/buyerTransferredFunds/{id?}") {
            coroutineScope {
                val orderId = call.parameters["id"] ?: ""
                val result = try {
                    async { buyerHasTransferredFundsToMerchant(buyOrderID = orderId) }.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = "An expected error has occurred please try again."
                )

                call.respond(
                    status = HttpStatusCode.OK,
                    message = result
                )
            }
        }
    }

}
fun Route.cancelBuyOrder() {
    authenticate("auth-jwt") {
        get("/cancelOrder/{id?}") {
            coroutineScope {

                val buyOrderId = call.parameters["id"] ?: ""
                val result = try {
                    async { cancelOrder(buyOrderId = buyOrderId) }.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = "An expected error has occurred please try again."
                )

                call.respond(
                    status = HttpStatusCode.OK,
                    message = result
                )
            }
        }
    }
}
fun Route.releaseCrypto() {
    authenticate("auth-jwt") {
        get("/releaseCrypto/{id?}") {
            coroutineScope {

                val buyOrderId = call.parameters["id"] ?: ""
                val result = try {
                    async { merchantReleaseCrypto(buyOrderID = buyOrderId) }.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = "An expected error has occurred please try again."
                )

                call.respond(
                    status = HttpStatusCode.OK,
                    message = result
                )
            }
        }
    }
}
