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
import online.pasaka.Kafka.KafkaProducer
import online.pasaka.model.merchant.wallet.MerchantFloatTopUp
import online.pasaka.model.merchant.wallet.MerchantWithdrawal
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

                   }catch (e:Exception){
                       e.printStackTrace()
                       "An expected error has occurred"
                   }
               }.await()
               when(result){
                   "Merchant registration was successful" -> {
                       call.respond(
                           message = MerchantRegistrationResponse(
                               status = true,
                               message = result
                           )
                       )
                   }
                   "Merchant registration failed" ->{
                       call.respond(
                           message = DefaultResponse(
                               status = false,
                               message = result
                           )
                       )
                   }
                   "Merchant already exists" ->{
                       call.respond(
                           message = DefaultResponse(
                               status = false,
                               message = result
                           )
                       )
                   }
                   "User doesn't exist" ->{
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

fun Route.merchantPaymentMethod(){

    authenticate("auth-jwt") {
        post("/addMerchantPaymentMethod") {
            coroutineScope {
                val principal = call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val paymentMethod = call.receive<PaymentMethod>()
                println(paymentMethod)
                val result = async(Dispatchers.IO) {
                    try {
                        MerchantServices.addMerchantPaymentMethod(
                            email = principal,
                            paymentMethod = paymentMethod
                        )

                    }catch (e:Exception){
                        e.printStackTrace()
                        ""
                    }
                }
                when(result.await()){
                    "Payment method added successfully" -> {
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = true,
                                message = result.await()
                            )
                        )
                    }
                    "Failed to add payment method" ->{
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message = result.await()
                            )
                        )
                    }
                    "" -> {
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message = result.await()
                            )
                        )
                    }
                }
            }
        }
    }



}

fun Route.merchantFloatTopUp(){
    authenticate("auth-jwt") {
        post("/merchantFloatTopUp") {
            coroutineScope {
                val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val merchantTopUp = call.receive<MerchantFloatTopUp>()

                val merchantTopUpProducer = try {
                    KafkaProducer().merchantTopUpProducer(
                        email = email,
                        topic = "MerchantFloatTopUp",
                        message = merchantTopUp
                    )
                    true
                }catch (e:Exception){
                    e.printStackTrace()
                    false
                }

                if (merchantTopUpProducer){
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = DefaultResponse(
                            status = true,
                            message =  "Float top-up was successful"
                        )
                    )
                }else {
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = DefaultResponse(
                            status = false,
                            message =  "Float top-up was not successful"
                        )
                    )
                }
            }

        }
    }
}

fun Route.merchantFloatWithdrawal(){
    authenticate("auth-jwt") {
        post("/merchantFloatWithdrawal") {
            coroutineScope {
                val email = call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val merchantWithdrawal = call.receive<MerchantWithdrawal>()
                val withdrawalFloat = async(Dispatchers.IO) {
                    try {
                        MerchantServices.merchantWithdrawalFloat(
                            email = email,
                            amount = merchantWithdrawal.amount,
                            currency = merchantWithdrawal.currency
                        )
                    }catch (e:Exception){
                        e.printStackTrace()
                        "An expected error has occurred"
                    }
                }
                when(val withdrawalResult = withdrawalFloat.await()){
                    "Float withdrawal was successful" ->{
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = true,
                                message =  withdrawalResult
                            )
                        )
                    }
                    "Float withdrawal was not successful" -> {
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message =  withdrawalResult
                            )
                        )
                    }
                    "Currency is not supported, use USD" -> {
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message =  withdrawalResult
                            )
                        )
                    }
                    "You have insufficient balance" ->{
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message =  withdrawalResult
                            )
                        )
                    }
                    "Merchant does not exist" -> {
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message =  withdrawalResult
                            )
                        )
                    }
                }
            }

        }
    }

}

fun Route.getMerchantFloatTopUpHistory(){
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
fun Route.getMerchantFloatWithdrawalHistory(){
    authenticate("auth-jwt") {
        get("/getMerchantFloatWithdrawalHistory") {
            coroutineScope {
                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                println(email)
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