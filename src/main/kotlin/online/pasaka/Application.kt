package online.pasaka

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.KafkaConsumers
import online.pasaka.plugins.*

suspend fun main() {
    coroutineScope {
        val serverJob = launch(Dispatchers.IO) {
            embeddedServer(
                Netty,
                port = (System.getenv("PORT") ?: "8888").toInt(),
                host = "localhost",
                module = Application::module
            )
                .start(wait = true)
        }

        // Launch Kafka consumer and embedded server concurrently
        val consumerJob = launch {
            KafkaConsumers().startConsumers()
        }



        // Wait for both jobs to complete
        consumerJob.join()
        serverJob.join()
    }
}


fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureRouting()
    configureClientLogging()

}


