package online.pasaka.plugins


import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import online.pasaka.routes.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World!")
        }
        cryptoPrices()
        cryptoPrice()
        userRegistration()
        getUserPortfolio()
        getUserData()
        signIn()
        updatePassword()
        sendOtp()

    }
}
