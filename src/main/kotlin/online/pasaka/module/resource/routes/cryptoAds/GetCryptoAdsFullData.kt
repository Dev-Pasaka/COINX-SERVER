package online.pasaka.module.resource.routes.cryptoAds

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.domain.repository.cache.RedisRepositoryImpl
import online.pasaka.domain.responses.*
import online.pasaka.domain.service.cryptoAds.CryptoAds

fun Route.getCryptoAdsFullData(){
    authenticate("auth-jwt") {
        get("/getCryptoAdsFullData/{adType?}") {
            coroutineScope {
                val orderType = call.parameters["adType"] ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(message = "Must include parameters in you request header e.g sellAd or buyAd")
                )
                val getDataInRedis = try {
                    async(Dispatchers.IO) {
                        RedisRepositoryImpl().getData(key = "/getCryptoBuyAdFullData/$orderType")
                    }.await()
                } catch (e: Exception) {
                    null
                }

                if (getDataInRedis != null) {
                    val resultJson = getDataInRedis.removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    when (orderType) {
                        "sellAd" -> {
                            call.respond(
                                status = HttpStatusCode.OK,
                                message = Json.decodeFromString<GetSellAdsFullDataResponse>(resultJson)
                            )
                        }
                        "buyAd" -> {
                            call.respond(
                                status = HttpStatusCode.OK,
                                message = Json.decodeFromString<GetBuyAdsFullDataResponse>(resultJson)
                            )
                        }
                    }
                }

                when (orderType) {
                    "sellAd" -> {
                        val sellAdResult = CryptoAds.getCryptoSellAdsFullData()

                        launch(Dispatchers.IO) {
                            RedisRepositoryImpl().setData(
                                key = "/getCryptoBuyAdFullData/$orderType",
                                data = sellAdResult,
                                expiresAt = 60
                            )
                        }
                        call.respond(status = HttpStatusCode.OK, message = sellAdResult)
                    }

                    "buyAd" -> {
                        val result = CryptoAds.getCryptoBuyAdsFullData()
                        launch(Dispatchers.IO) {
                            RedisRepositoryImpl().setData(
                                key = "/getCryptoBuyAdFullData/$orderType",
                                data = result,
                                expiresAt = 60
                            )
                        }
                        call.respond(status = HttpStatusCode.OK, message = result)
                    }

                    else -> {
                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(message = "Wrong  parameters in you request header. (e.g sellAd or buyAd)")
                        )
                    }

                }


            }
        }
    }
}