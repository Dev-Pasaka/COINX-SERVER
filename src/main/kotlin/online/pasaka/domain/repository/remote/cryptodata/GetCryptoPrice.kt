package online.pasaka.domain.repository.remote.cryptodata

import com.google.gson.Gson
import online.pasaka.domain.model.wallet.crypto.Cryptocurrency
import kotlinx.serialization.json.*
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import online.pasaka.infrastructure.config.Config

class GetCryptoPrice {

    suspend fun getCryptoMetadata(cryptoSymbol: String = "ETH", currency: String = "USD"): Cryptocurrency {
        return try {

            val gson = Gson()
            val config = Config.load
            val url = config.property("coinmarketcap_quote_url").getString()
            val client = HttpClient(CIO)
            val request = client.get(url) {
                header("X-CMC_PRO_API_KEY", config.property("coinmarketcap_apikey").getString())
                url {
                    parameter("symbol", cryptoSymbol)
                    parameter("convert", currency)
                }
            }

            val jsonResponseString = request.bodyAsText()
            println(jsonResponseString)
            val jsonResponseObj = Json.parseToJsonElement(jsonResponseString) as JsonObject
            val cryptoData = jsonResponseObj["data"]
            val cryptoObject = Json.parseToJsonElement(cryptoData.toString()) as JsonObject
            val crypto = cryptoObject[cryptoSymbol] as? Map<*, *>
            val cryptoPriceQuoteObj = Json.parseToJsonElement(crypto.toString()) as JsonObject
            val cryptoQuote = cryptoPriceQuoteObj["quote"]
            val cryptoKesObj = Json.parseToJsonElement(cryptoQuote.toString()) as JsonObject
            val cryptoInKes = cryptoKesObj[currency] as Map<String, Double>

            Cryptocurrency(
                id = crypto?.get("id")?.toString()?.toInt() ?: 0,
                name = crypto?.get("name")?.toString() ?: "",
                symbol = crypto?.get("symbol")?.toString() ?: "",
                price = cryptoInKes["price"].toString(),
                volume24h = cryptoInKes["volume_24h"].toString(),
                volumeChange24h = cryptoInKes["volume_change_24h"].toString(),
                percentageChange1h = cryptoInKes["percent_change_1h"].toString(),
                percentageChange24h = (Math.round((cryptoInKes["percent_change_24h"].toString()).toDouble()) * 100 / 10000.0).toString(),
                percentageChange7d = cryptoInKes["percent_change_7d"].toString(),
                percentageChange30d = cryptoInKes["percent_change_30d"].toString(),
                percentageChange60d = cryptoInKes["percent_change_60d"].toString(),
                percentageChange90d = cryptoInKes["percent_change_90d"].toString(),
                marketCap = cryptoInKes["market_cap"].toString(),
                fullyDilutedMarketCap = cryptoInKes["fully_diluted_market_cap"].toString()

            )
        } catch (e: Exception) {
            Cryptocurrency(
                id = 0,
                name = "",
                symbol = "",
                price = "",
                volume24h = "",
                volumeChange24h = "",
                percentageChange1h = "",
                percentageChange24h = "",
                percentageChange7d = "",
                percentageChange30d = null,
                percentageChange60d = "",
                percentageChange90d = "",
                marketCap = "",
                fullyDilutedMarketCap = ""

            )
        }
    }


    }
