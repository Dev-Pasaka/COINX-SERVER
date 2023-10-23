package online.pasaka.resource.serverSentEvents

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

fun Route.sentEvents(){
    get("/sse"){
        call.respondSseEvents()
    }
}

suspend fun ApplicationCall.respondSseEvents() {
    val eventChannel = produceEvents() // Implement this function to produce your SSE events

    respondTextWriter(contentType = ContentType.Text.EventStream) {
        try {

//            eventChannel.collect { event ->
//                // Send SSE event to the client
//                append("data: ${event}\n\n")
//                flush()
//            }

            while (true){
                delay(1000)
                append((100..11100).random().toString())
                flush()
            }
        } catch (e: ClosedReceiveChannelException) {
            // Channel is closed, handle it as needed
        } finally {

        }
    }
}

suspend fun produceEvents(): Flow<Int> = flow {
    while (true) {
        // Generate an SSE event
        val event = (100..10000).random()

        // Emit the event to the channel
        emit(event)

        // Delay between events (optional)
        delay(1000)
    }
}