package online.pasaka.plugins


import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import online.pasaka.resource.routes.*
import online.pasaka.resource.serverSentEvents.sentEvents
import online.pasaka.resource.websokets.merchantLastSeen

fun Application.configureRouting() {
    routing {

        get("/") {
            val targetUrl = "https://coinx.co.ke"
            call.respondRedirect(targetUrl, permanent = false)
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
        deleteAccount()
        createBuyAd()
        createSellAd()
        merchantCryptoSwap()
        sentEvents()
        cryptoBuyOrder()
        buyerTransferredFunds()
        cancelBuyOrder()
        merchantReleaseCrypto()
        cryptoSellOrder()
        merchantTransferredFunds()
        cancelSellOrder()
        sellerReleaseCrypto()
    }
}
