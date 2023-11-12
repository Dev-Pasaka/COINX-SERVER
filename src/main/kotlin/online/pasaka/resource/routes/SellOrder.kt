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
import online.pasaka.Kafka.models.messages.SellOrderMessage
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.dto.SellOrderDto
import online.pasaka.responses.DefaultResponse
import online.pasaka.service.sellOrderService.merchantHasTransferredFunds
import online.pasaka.service.sellOrderService.cancelSellOrder
import online.pasaka.service.sellOrderService.sellerReleaseCrypto
fun Route.cryptoSellOrder() {
    authenticate("auth-jwt") {

        post("/cryptoSellOrder") {

            coroutineScope {

                val email = call.principal<JWTPrincipal>()
                    ?.payload
                    ?.getClaim("email")
                    .toString()
                    .removeSurrounding("\"")

                val sellOrder = call.receive<SellOrderDto>()
                val sellOrderMessage = SellOrderMessage(
                    adId = sellOrder.adId,
                    sellersEmail = email,
                    cryptoName = sellOrder.cryptoName,
                    cryptoSymbol = sellOrder.cryptoSymbol,
                    cryptoAmount = sellOrder.cryptoAmount,
                )
                val gson = Gson()
                val sellOrderJsonString = gson.toJson(sellOrderMessage)

                try {
                    launch { kafkaProducer(topic = KafkaConfig.CRYPTO_SELL_ORDERS, message = sellOrderJsonString) }
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
                        message = "Your sell order has been placed successfully "
                    )
                )


            }


        }

    }

}
fun Route.merchantTransferredFunds() {

    authenticate("auth-jwt") {
        get("/merchantTransferredFunds/{id?}") {
            coroutineScope {
                val orderId = call.parameters["id"] ?: ""
                val result = try {
                    async { merchantHasTransferredFunds(sellOrderID = orderId) }.await()
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                } ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = "An expected error has occurred, check you parameters and please try again."
                )

                call.respond(
                    status = HttpStatusCode.OK,
                    message = result
                )
            }
        }
    }

}
fun Route.cancelSellOrder() {
    authenticate("auth-jwt") {
        get("/cancelSellOrder/{id?}") {
            coroutineScope {

                val sellOrderId = call.parameters["id"] ?: ""
                val result = try {
                    async { cancelSellOrder(sellOrderId = sellOrderId ) }.await()
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
fun Route.sellerReleaseCrypto() {
    authenticate("auth-jwt") {
        get("/sellerReleaseCrypto/{id?}") {
            coroutineScope {

                val sellOrderId = call.parameters["id"] ?: ""
                val result = try {
                    async {sellerReleaseCrypto(sellOrderId = sellOrderId) }.await()
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
