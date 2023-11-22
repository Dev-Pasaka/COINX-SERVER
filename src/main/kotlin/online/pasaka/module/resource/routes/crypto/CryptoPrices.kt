package online.pasaka.module.resource.routes.crypto

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import online.pasaka.domain.model.wallet.crypto.Cryptocurrency
import online.pasaka.domain.repository.cache.RedisRepositoryImpl
import online.pasaka.domain.repository.remote.cryptodata.GetAllCryptoPrices
import online.pasaka.domain.repository.remote.cryptodata.GetCryptoPrice
import online.pasaka.infrastructure.config.Redis

fun Route.cryptoPrices(){
    authenticate("auth-jwt") {
        get("/cryptoPrices") {
            coroutineScope {
                val getAllCryptoDataInRedis = RedisRepositoryImpl().getData(key = Redis.CRYPTO_PRICES)

                if (getAllCryptoDataInRedis != null){
                    val resultJson = getAllCryptoDataInRedis
                    val resultObj = Json.decodeFromString<List<Cryptocurrency>>(resultJson)
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = resultObj
                    )
                }else{
                    val getAllCryptoPrices = GetAllCryptoPrices().getAllCryptoMetadata()
                    RedisRepositoryImpl().setData(key = Redis.CRYPTO_PRICES, data = getAllCryptoPrices)
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = getAllCryptoPrices
                    )
                }

            }
        }
    }
}
fun Route.cryptoPrice(){
    authenticate("auth-jwt") {

        get("/cryptoPrice/{symbol?}") {
            coroutineScope {
                val symbol = call.parameters["symbol"]?.uppercase() ?: ""

                val getAllCryptoDataInRedis = RedisRepositoryImpl().getData(key = symbol)

                if (getAllCryptoDataInRedis != null){
                    val resultJson = getAllCryptoDataInRedis
                    val resultObj = Json.decodeFromString<Cryptocurrency>(resultJson)
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = resultObj
                    )
                }else{
                    val getCryptoPrice = GetCryptoPrice().getCryptoMetadata(cryptoSymbol = symbol, currency = "KES")
                    RedisRepositoryImpl().setData(key = symbol, data = getCryptoPrice)
                    call.respond(
                        status = HttpStatusCode.OK,
                        message = getCryptoPrice
                    )
                }
            }

        }
    }
}


