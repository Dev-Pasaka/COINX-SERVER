package online.pasaka

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.Kafka.consumers.startConsumers
import online.pasaka.plugins.*
import online.pasaka.scheduleTasks.startTasksSchedulers

suspend fun main() {

    coroutineScope {

        val serverJob = launch(Dispatchers.IO) {

            embeddedServer(
                Netty,
                port = (System.getenv("PORT") ?: "8080").toInt(),
                host = "0.0.0.0",
                module = Application::module
            ).start(wait = true)

        }

        val consumerJob = launch { startConsumers() }
        val taskSchedulersJob = launch { startTasksSchedulers() }

        consumerJob.join()
        serverJob.join()
        taskSchedulersJob.join()

    }
}


fun Application.module() {

    configureSerialization()
    configureSecurity()
    configureRouting()
    configureClientLogging()
    configureWebsocket()

}


