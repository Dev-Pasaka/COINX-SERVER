package online.pasaka.resource.routes

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
import online.pasaka.Kafka.KafkaProducer
import online.pasaka.config.KafkaConfig
import online.pasaka.model.cryptoAds.CryptoBuyAdOrder
import online.pasaka.dto.CryptoBuyAdOrderDto
import online.pasaka.dto.CryptoSellAdOrderDto
import online.pasaka.model.cryptoAds.CryptoSellAdOrder
import online.pasaka.model.merchant.wallet.MerchantCryptoSwap
import online.pasaka.model.merchant.wallet.crypto.CryptoSwap
import online.pasaka.model.user.PaymentMethod
import online.pasaka.responses.DefaultResponse
import online.pasaka.responses.MerchantFloatTopUpTransactionsHistoryResponse
import online.pasaka.responses.MerchantFloatWithdrawalHistoryResponse
import online.pasaka.responses.MerchantRegistrationResponse
import online.pasaka.service.MerchantServices


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
                val paymentMethod = call.receive<PaymentMethod>()

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

                            KafkaProducer().merchantTopUpProducer(
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
                            KafkaProducer().merchantWithdrawalProducer(
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

    authenticate("auth-jwt") {

        get("/getMerchantFloatTopUpHistory") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                println(email)

                val result = try {

                    MerchantServices.getMerchantFloatTopUpHistory(email = email)

                } catch (e: Exception) {

                    e.printStackTrace()
                    listOf()

                }
                if (result.isNotEmpty()) {

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
                            body = result
                        )
                    )

                }
            }
        }
    }
}

fun Route.getMerchantFloatWithdrawalHistory() {

    authenticate("auth-jwt") {

        get("/getMerchantFloatWithdrawalHistory") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")

                val result = try {

                    MerchantServices.getMerchantFloatWithdrawalHistory(email = email)

                } catch (e: Exception) {

                    e.printStackTrace()
                    listOf()

                }

                if (result.isNotEmpty()) {

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
                        message = MerchantFloatWithdrawalHistoryResponse(
                            status = false,
                            body = result

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
                val cryptoBuyAdOrder = call.receive<CryptoBuyAdOrderDto>()

                val result = async {
                    try {
                        MerchantServices.createBuyAd(cryptoBuyAdOrder = CryptoBuyAdOrder(
                            email = email,
                            cryptoName = cryptoBuyAdOrder.cryptoName,
                            cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                            totalAmount = cryptoBuyAdOrder.totalAmount,
                            margin = cryptoBuyAdOrder.margin,
                            minLimit = cryptoBuyAdOrder.minLimit,
                            maxLimit = cryptoBuyAdOrder.maxLimit
                        ))
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
                val cryptoBuyAdOrder = call.receive<CryptoSellAdOrderDto>()

                val result = async {
                    try {
                        MerchantServices.createSellAd(cryptoSellAdOrder = CryptoSellAdOrder(
                            email = email,
                            cryptoName = cryptoBuyAdOrder.cryptoName,
                            cryptoSymbol = cryptoBuyAdOrder.cryptoSymbol,
                            totalAmount = cryptoBuyAdOrder.totalAmount,
                            margin = cryptoBuyAdOrder.margin,
                            minLimit = cryptoBuyAdOrder.minLimit,
                            maxLimit = cryptoBuyAdOrder.maxLimit
                        ))
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
                val swapOrder = call.receive<MerchantCryptoSwap>()

                val result = async {
                    try {
                        MerchantServices.swapCrypto(
                            CryptoSwap(
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
