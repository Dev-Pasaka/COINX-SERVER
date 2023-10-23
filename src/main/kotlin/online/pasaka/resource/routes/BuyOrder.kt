package online.pasaka.resource.routes

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.models.BuyOrderConfirmationNotificationMessage
import online.pasaka.Kafka.models.BuyOrderMessage
import online.pasaka.Kafka.models.Notification
import online.pasaka.Kafka.models.NotificationType
import online.pasaka.Kafka.producers.kafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.dto.BuyOrderDto
import online.pasaka.responses.DefaultResponse


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