package online.pasaka.module.resource.routes.cryptoAds

import com.google.gson.Gson
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.domain.repository.cache.RedisRepositoryImpl
import online.pasaka.domain.responses.DefaultResponse
import online.pasaka.domain.responses.GetCryptoBuyAdsResponse
import online.pasaka.domain.responses.GetCryptoSellAdsResponse
import online.pasaka.domain.service.cryptoAds.CryptoAds

fun Route.getCryptoAds() {
    authenticate("auth-jwt") {
        get("/getCryptoAd/{adType?}") {
            coroutineScope {
                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")
                val orderType = call.parameters["adType"] ?: call.respond(
                    status = HttpStatusCode.OK,
                    message = DefaultResponse(message = "Must include parameters in you request header e.g sellAd or buyAd")
                )
                val getDataInRedis = try {
                    async(Dispatchers.IO) {
                        RedisRepositoryImpl().getData(key = "/getCryptoAd/$orderType")
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
                                message = Json.decodeFromString<GetCryptoSellAdsResponse>(resultJson)
                            )
                        }
                        "buyAd" -> {
                            call.respond(
                                status = HttpStatusCode.OK,
                                message = Json.decodeFromString<GetCryptoBuyAdsResponse>(resultJson)
                            )
                        }
                    }
                }

                when (orderType) {
                    "sellAd" -> {
                        val sellAdResult = CryptoAds.getCryptoSellAds()
                        sellAdResult.message

                        launch(Dispatchers.IO) {
                            RedisRepositoryImpl().setData(
                                key = "/getCryptoAd/$orderType",
                                data = sellAdResult,
                                expiresAt = 60
                            )
                        }
                        call.respond(status = HttpStatusCode.OK, message = sellAdResult)
                    }

                    "buyAd" -> {
                        val result = CryptoAds.getCryptoBuyAds()
                        launch(Dispatchers.IO) {
                            RedisRepositoryImpl().setData(
                                key = "/getCryptoAd/$orderType",
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