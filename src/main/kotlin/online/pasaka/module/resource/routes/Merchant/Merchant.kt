package online.pasaka.module.resource.routes.Merchant

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.Kafka.producers.merchantTopUpProducer
import online.pasaka.Kafka.producers.merchantWithdrawalProducer
import online.pasaka.domain.dto.merchant.MerchantDataDto
import online.pasaka.domain.repository.cache.RedisRepositoryImpl
import online.pasaka.domain.repository.database.merchants.MerchantRepositoryImpl
import online.pasaka.infrastructure.config.KafkaConfig
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.responses.MerchantFloatTopUpTransactionsHistoryResponse
import online.pasaka.domain.responses.MerchantFloatWithdrawalHistoryResponse
import online.pasaka.domain.responses.MerchantRegistrationResponse
import online.pasaka.domain.service.merchant.MerchantServices


fun Route.becomeMerchant() {
    authenticate("auth-jwt") {

        get("/becomeMerchant") {

            coroutineScope {

                val principal = call.principal<JWTPrincipal>()
                println(principal?.payload?.getClaim("email"))

                val result = async(Dispatchers.IO) {

                    try {

                        MerchantServices.becomeMerchant(
                            email = principal?.payload?.getClaim("email").toString().removeSurrounding("\""),
                        )

                    } catch (e: Exception) {
                        e.printStackTrace()
                        "An expected error has occurred"
                    }
                }.await()

                when (result) {
                    "Merchant registration was successful" -> {

                        call.respond(
                            message = MerchantRegistrationResponse(
                                status = true,
                                message = result
                            )
                        )

                    }

                    "Merchant registration failed" -> {

                        call.respond(
                            message = DefaultResponse(
                                status = false,
                                message = result
                            )
                        )

                    }

                    "Merchant already exist" -> {
                        call.respond(
                            message = DefaultResponse(
                                status = false,
                                message = result
                            )
                        )
                    }

                    "Merchant Wallet Already Exists" -> {
                        call.respond(
                            message = DefaultResponse(
                                status = false,
                                message = result
                            )
                        )
                    }

                    "User doesn't exist" -> {

                        call.respond(
                            message = DefaultResponse(
                                status = false,
                                message = result
                            )
                        )

                    }
                }
            }
        }
    }
}
fun Route.merchantPaymentMethod() {

    authenticate("auth-jwt") {

        post("/addMerchantPaymentMethod") {

            coroutineScope {

                val principal =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val paymentMethod = call.receive<online.pasaka.domain.model.user.PaymentMethod>()

                val result = async(Dispatchers.IO) {

                    try {

                        MerchantServices.addMerchantPaymentMethod(
                            email = principal,
                            paymentMethod = paymentMethod
                        )

                    } catch (e: Exception) {

                        e.printStackTrace()
                        ""

                    }
                }.await()
                when (result) {

                    "Payment method added successfully" -> {

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = true,
                                message = result
                            )
                        )

                    }

                    "Failed to add payment method" -> {

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message = result
                            )
                        )

                    }

                    "Merchant does not exist" -> {

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message = result
                            )
                        )

                    }
                }
            }
        }
    }


}
fun Route.merchantFloatTopUp() {

    authenticate("auth-jwt") {

        get("/merchantFloatTopUp/{crypto?}") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val amount = call.parameters["crypto"] ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        message = "Wrong parameter format",
                        status = false
                    )
                )

                try {
                    amount.toString().toDouble()
                } catch (e: Exception) {
                    null
                } ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        message = "Parameters passed couldn't be parsed to double e.g 0.0",
                        status = false
                    )
                )

                launch(Dispatchers.IO) {

                    try {
                        if (amount.toString().toDoubleOrNull() != null) {
                            merchantTopUpProducer(
                                email = email,
                                topic = KafkaConfig.MERCHANT_FLOAT_TOP_UP,
                                message = amount.toString()
                            )
                        }
                    } catch (e: Exception) {

                        e.printStackTrace()

                    }
                }

                call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = true,
                        message = "Float top-up order was successfully placed"
                    )
                )

            }

        }
    }
}
fun Route.merchantFloatWithdrawal() {

    authenticate("auth-jwt") {

        get("/merchantFloatWithdrawal/{crypto?}") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val merchantWithdrawal = call.parameters["crypto"] ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        message = "Wrong parameter format",
                        status = false
                    )
                )

                launch(Dispatchers.IO) {

                    try {
                        if (merchantWithdrawal.toString().toDoubleOrNull() != null) {
                            merchantWithdrawalProducer(
                                email = email,
                                topic = KafkaConfig.MERCHANT_FLOAT_WITHDRAWAL,
                                message = merchantWithdrawal.toString(),
                            )
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                }

                call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = true,
                        message = "Float withdrawal order  was successfully placed"
                    )
                )

            }
        }

    }
}
fun Route.getMerchantFloatTopUpHistory() {
val gson = Gson()
    authenticate("auth-jwt") {

        get("/getMerchantFloatTopUpHistory/{allHistory?}") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val getAllHistory = call.parameters["allHistory"] ?: "false"

                val getMerchantFloatTopUpHistory = async(Dispatchers.IO) { RedisRepositoryImpl().getData(key = "/getMerchantFloatTopUpHistory$email") }.await()
                if (getMerchantFloatTopUpHistory != null){
                    val result = getMerchantFloatTopUpHistory.removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    call.respond(status = HttpStatusCode.OK,message = result)
                }

                val result = MerchantRepositoryImpl().getMerchantFloatTopUpHistory(email = email, allHistory = getAllHistory)

                if (result?.isNotEmpty() == true) {
                    val response = MerchantFloatTopUpTransactionsHistoryResponse(
                        status = true,
                        body = result
                    )
                    launch(Dispatchers.IO) {
                        RedisRepositoryImpl().setData(key = "/getMerchantFloatTopUpHistory$email", gson.toJson(response))
                    }
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = MerchantFloatTopUpTransactionsHistoryResponse(
                            status = true,
                            body = result
                        )
                    )
                } else {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = MerchantFloatTopUpTransactionsHistoryResponse(
                            status = false,
                            body = emptyList()
                        )
                    )

                }
            }
        }
    }
}
fun Route.getMerchantFloatWithdrawalHistory() {
    val gson = Gson()
    authenticate("auth-jwt") {

        get("/getMerchantFloatWithdrawalHistory") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val getAllHistory = call.parameters["allHistory"] ?: "false"

                val getMerchantFloatWithdrawalHistory = async(Dispatchers.IO) { RedisRepositoryImpl().getData(key = "/getMerchantFloatWithdrawalHistory$email") }.await()
                if (getMerchantFloatWithdrawalHistory != null){
                    val result = getMerchantFloatWithdrawalHistory.removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    call.respond(status = HttpStatusCode.OK,message = result)
                }

                val result = MerchantRepositoryImpl().getMerchantFloatWithdrawalHistory(email = email, allHistory = getAllHistory)

                if (result?.isNotEmpty() == true) {
                    val response = MerchantFloatWithdrawalHistoryResponse(
                        status = true,
                        body = result
                    )
                    launch(Dispatchers.IO) {
                        RedisRepositoryImpl().setData(key = "/getMerchantFloatWithdrawalHistory$email", gson.toJson(response))
                    }
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = MerchantFloatWithdrawalHistoryResponse(
                            status = true,
                            body = result
                        )
                    )
                } else {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = MerchantFloatTopUpTransactionsHistoryResponse(
                            status = false,
                            body = emptyList()
                        )
                    )

                }


            }
        }
    }
}
fun Route.createBuyAd() {
    authenticate("auth-jwt") {

        post("/createBuyAd") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val cryptoBuyAdOrder = call.receive<online.pasaka.domain.dto.CryptoBuyAdOrderDto>()

                val result = async {
                    try {
                        MerchantServices.createBuyAd(cryptoBuyAdOrder = online.pasaka.domain.dto.cryptoAds.CreateBuyAdDto(
                            email = email,
                            cryptoName = cryptoBuyAdOrder.cryptoName,
                            cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                            totalAmount = cryptoBuyAdOrder.totalAmount,
                            margin = cryptoBuyAdOrder.margin,
                            minLimit = cryptoBuyAdOrder.minLimit,
                            maxLimit = cryptoBuyAdOrder.maxLimit
                        )
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.await() ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error has occurred, try again latter"
                    )
                )

                call.respond(status = HttpStatusCode.OK, message = result)


            }
        }
    }
}
fun Route.createSellAd() {
    authenticate("auth-jwt") {

        post("/createSellAd") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val cryptoBuyAdOrder = call.receive<online.pasaka.domain.dto.CryptoSellAdOrderDto>()

                val result = async {
                    try {
                        MerchantServices.createSellAd(cryptoSellAdOrder = online.pasaka.domain.dto.cryptoAds.CreateSellAdDto(
                            email = email,
                            cryptoName = cryptoBuyAdOrder.cryptoName,
                            cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                            totalAmount = cryptoBuyAdOrder.totalAmount,
                            margin = cryptoBuyAdOrder.margin,
                            minLimit = cryptoBuyAdOrder.minLimit,
                            maxLimit = cryptoBuyAdOrder.maxLimit
                        )
                        )
                    } catch (e: Exception) {
                        null
                    }
                }.await() ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = false,
                        message = "An expected error has occurred, try again latter"
                    )
                )

                call.respond(status = HttpStatusCode.OK, message = result)


            }
        }
    }
}
fun Route.merchantCryptoSwap() {
    authenticate("auth-jwt") {

        post("/merchantCryptoSwap") {

            coroutineScope {
                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val swapOrder = call.receive<online.pasaka.domain.model.merchant.wallet.MerchantCryptoSwap>()

                val result = async {
                    try {
                        MerchantServices.swapCrypto(
                            online.pasaka.domain.model.merchant.wallet.crypto.CryptoSwap(
                                email = email,
                                cryptoAmount = swapOrder.cryptoAmount,
                                toCryptoSymbol = swapOrder.toCryptoSymbol,
                                fromCryptoSymbol = swapOrder.fromCryptoSymbol
                            )
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null
                    }
                }.await() ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(
                        status = false,
                        message = "Internal error occurred Swapping failed"
                    )
                )

                call.respond(status = HttpStatusCode.OK, message = result)


            }
        }


    }
}
fun Route.getMerchantData(){
    authenticate("auth-jwt") {
        get("/getMerchantData") {
            val email =
                call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")

            val getMerchantDataInRedis = RedisRepositoryImpl().getData(key = "/getMerchantData$email")
            if (getMerchantDataInRedis != null){
                val resultObj = Json.decodeFromString<MerchantDataDto>(getMerchantDataInRedis)
                call.respond(
                    status = HttpStatusCode.OK,
                    message = resultObj
                )
            }

            val getMerchantData = MerchantRepositoryImpl().getMerchantData(email = email)
            if (getMerchantData != null){
                launch(Dispatchers.IO) {
                    RedisRepositoryImpl().setData(
                        key = "/getMerchantData$email",
                        data = getMerchantData,
                        expiresAt = 60
                    )
                }
                call.respond(
                    status = HttpStatusCode.OK,
                    message = getMerchantData
                )

            }else{
                call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(message = "Failed to get merchant data")
                )
            }



        }
    }
}
