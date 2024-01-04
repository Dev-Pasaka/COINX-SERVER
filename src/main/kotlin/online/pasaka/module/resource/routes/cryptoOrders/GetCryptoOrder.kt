package online.pasaka.module.resource.routes.cryptoOrders

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import online.pasaka.domain.repository.database.orders.OrdersRepositoryImpl
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.service.orders.Orders

fun Route.getCryptoOrder() {
    authenticate("auth-jwt") {

        get("/getCryptoOrder/{orderType?}") {
            val email = call.principal<JWTPrincipal>()
                ?.payload
                ?.getClaim("email")
                .toString()
                .removeSurrounding("\"")

            val orderType = call.parameters["orderType"] ?: call.respond(
                status = HttpStatusCode.OK,
                message = DefaultResponse(
                    message = "Parameter orderType can not be null or empty",
                    status = false
                )
            )

            when (orderType) {
                "buyOrder" -> call.respond(
                    status = HttpStatusCode.OK,
                    message = Orders.getBuyOrder(email = email)
                )

                "sellOrder" -> call.respond(
                    status = HttpStatusCode.OK,
                    message = Orders.getSellOrder(email = email)
                )
                else ->{
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = DefaultResponse(
                            message = "Wrong parameters kindly choose either sellOrder or buyOrder",
                            status = false
                        )
                    )
                }
            }

        }
    }
}


