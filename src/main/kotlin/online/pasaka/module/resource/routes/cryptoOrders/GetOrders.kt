package online.pasaka.module.resource.routes.cryptoOrders

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.service.orders.Orders

fun Route.getOrders(){
    authenticate("auth-jwt") {
        val gson = Gson()
        get("/getOrders/{orderType?}") {
            coroutineScope {
                val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val orderType = call.parameters["orderType"]  ?:
                call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(message = "Must include parameters in you request header e.g sellOrders or buyOrders")
                )

                when(orderType){
                    "buyOrder" -> {
                        val result = Orders.getBuyOrders(email = email)
                        call.respond(status = HttpStatusCode.OK, message = gson.toJson(result))
                    }
                    "sellOrder" ->{
                        val result = Orders.getSellOrders(email = email)
                        call.respond(status = HttpStatusCode.OK, message = gson.toJson(result))
                    }
                    else ->{
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(message = "Wrong  parameters in you request header. (e.g sellAd or buyAd)")
                        )
                    }

                }

            }
        }
    }
}