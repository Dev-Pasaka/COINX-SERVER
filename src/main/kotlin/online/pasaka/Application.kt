package online.pasaka

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import online.pasaka.plugins.*

fun main() {
    embeddedServer(Netty, port = (System.getenv("PORT")?:"8888").toInt(), host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

 fun Application.module() {
    configureSerialization()
    configureSecurity()
    configureRouting()
}
