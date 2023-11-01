package online.pasaka.resource.serverSentEvents

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import online.pasaka.database.Entries
import online.pasaka.model.order.BuyOrder
import online.pasaka.model.order.OrderStatus
import online.pasaka.threads.Threads
import org.litote.kmongo.eq
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

fun Route.sentEvents() {
    val gson = Gson()
    authenticate("auth-jwt") {
        get("/sse") {
            coroutineScope{
               launch {
                   val email = call.principal<JWTPrincipal>()
                       ?.payload
                       ?.getClaim("email")
                       .toString()
                       .removeSurrounding("\"")

                   call.respondTextWriter(contentType = ContentType.Text.EventStream) {

                       while (true) {
                           val buyersOrders = try {
                               Entries.cryptoBuyOrders.find(BuyOrder::buyersEmail eq email).toList()
                           }catch (e:Exception){
                               e.printStackTrace()
                               null
                           } ?: continue
                           val event = gson.toJson(buyersOrders)
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

fun main(){
    println(
        Entries.cryptoBuyOrders.find(BuyOrder::buyersEmail eq "dev.pasaka@gmail.com").toList()
    )
}
