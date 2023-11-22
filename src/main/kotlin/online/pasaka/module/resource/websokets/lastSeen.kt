package online.pasaka.module.resource.websokets

import com.google.gson.Gson
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.service.merchant.updateLastSeenStatus
import online.pasaka.domain.utils.Utils

fun Route.merchantLastSeen() {
    val gson = Gson()
    authenticate("auth-jwt") {
            webSocket("/lastSeen"){
                val email = call.principal<JWTPrincipal>()
                    ?.payload
                    ?.getClaim("email")
                    .toString()
                    .removeSurrounding("\"")
                send("You are connected!")

                for(frame in incoming) {
                    frame as? Frame.Text ?: continue

                    if (frame.readText() == "true"){
                        val updateLastSeen = updateLastSeenStatus(email = email, lastSeen = Utils.currentTimeStamp())
                        val response = gson.toJson(updateLastSeen)
                        send(response)
                    }else{
                        val response = DefaultResponse("Wrong body use e.g true in string/text")
                        send(gson.toJson(response))
                    }

                }
            }
        }
}
