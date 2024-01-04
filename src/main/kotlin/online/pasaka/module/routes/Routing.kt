package online.pasaka.module.routes


import io.ktor.server.routing.*
import io.ktor.server.response.*
import io.ktor.server.application.*
import online.pasaka.module.resource.routes.Merchant.*
import online.pasaka.module.resource.routes.User.*
import online.pasaka.module.resource.routes.cryptoOrders.*
import online.pasaka.module.resource.routes.crypto.cryptoPrice
import online.pasaka.module.resource.routes.crypto.cryptoPrices
import online.pasaka.module.resource.routes.cryptoAds.getCryptoAds
import online.pasaka.module.resource.routes.cryptoAds.getCryptoAdsFullData
import online.pasaka.module.resource.serverSentEvents.liveOrderStatus

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
        liveOrderStatus()
        cryptoBuyOrder()
        buyerTransferredFunds()
        cancelBuyOrder()
        merchantReleaseCrypto()
        cryptoSellOrder()
        merchantTransferredFunds()
        cancelSellOrder()
        sellerReleaseCrypto()
        getOrders()
        getCryptoAds()
        getMerchantData()
        getCryptoAdsFullData()
        getCryptoOrder()
    }
}
