package online.pasaka.plugins


import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import online.pasaka.resource.routes.*
import online.pasaka.routes.*

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondText("Hello World! pasaka")
        }
        cryptoPrices()
        cryptoPrice()
        userRegistration()
        getUserPortfolio()
        getUserData()
        signIn()
        verifyPhone()
        updatePassword()
        becomeMerchant()
        merchantPaymentMethod()
        merchantFloatTopUp()
        merchantFloatWithdrawal()
        getMerchantFloatTopUpHistory()
        getMerchantFloatWithdrawalHistory()

    }
}
