package online.pasaka

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import online.pasaka.Kafka.consumers.startConsumers
import online.pasaka.module.plugins.*
import online.pasaka.module.routes.configureRouting
import online.pasaka.infrastructure.scheduleTasks.startTasksSchedulers

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {

    coroutineScope {
        val serverJob = launch {
            embeddedServer(
                Netty,
                port = (System.getenv("PORT") ?: "8080").toInt(),
                host = "0.0.0.0",
                module = Application::module
            ).start(wait = true)
        }
        val consumerJob = launch { startConsumers() }
        val taskSchedulersJob = launch { startTasksSchedulers() }
        serverJob.join()
        consumerJob.join()
        taskSchedulersJob.join()
    }
}


fun Application.module() {

    configureSerialization()
    configureSecurity()
    configureRouting()
    configureClientLogging()
    configureWebsocket()
    configureRateLimiting()

}


