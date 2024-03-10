package online.pasaka

import io.ktor.network.tls.certificates.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.*
import online.pasaka.Kafka.consumers.startConsumers
import online.pasaka.module.plugins.*
import online.pasaka.module.routes.configureRouting
import online.pasaka.infrastructure.scheduleTasks.startTasksSchedulers
import java.io.File
import java.util.logging.Level

@OptIn(DelicateCoroutinesApi::class)
suspend fun main() {

    generateSelfSignedCertificate()
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
    environment.log.isInfoEnabled()
}

fun generateSelfSignedCertificate() {
    val keyStoreFile = File("build/keystore.jks")
    val keyStore = buildKeyStore {
        certificate("sampleAlias") {
            password = "foobar"
            domains = listOf("127.0.0.1", "0.0.0.0", "localhost")
        }
    }
    keyStore.saveToFile(keyStoreFile, "123456")
}


