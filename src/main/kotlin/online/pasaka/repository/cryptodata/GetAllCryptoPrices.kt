package online.pasaka.repository.cryptodata

import online.pasaka.model.wallet.crypto.Cryptocurrency
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import online.pasaka.config.Config


class GetAllCryptoPrices {
    suspend fun getAllCryptoMetadata(): List<Cryptocurrency> {

        val config = Config.load
        val url = config.property("coinmarketcap_listing_url").getString()
        val client = HttpClient(CIO)
        val request = client.get(url){
            header("X-CMC_PRO_API_KEY", config.property("coinmarketcap_apikey").getString())
            //contentType(ContentType.Application.Json)
            url {
                parameter("start", "1")
                parameter("limit","50")
                parameter("convert","KES")
            }
        }

        val jsonResponseString:String = request.bodyAsText()
        val cryptoData:List<*>
        val jsonResponseObj = Json.parseToJsonElement(jsonResponseString) as JsonObject
        cryptoData = jsonResponseObj["data"] as List<*>
        val top10CryptoPrices = mutableListOf<Cryptocurrency>()

        for (data in cryptoData){
            val dataObj = data as Map<*,*>
            val id = dataObj["id"]
            val name = dataObj["name"]
            val symbol = dataObj["symbol"]
            val priceQuote = dataObj["quote"]
            val currencyObj = Json.parseToJsonElement(priceQuote.toString()) as JsonObject
            val currency = currencyObj["KES"] as Map<String,Double>
            top10CryptoPrices.add(
                Cryptocurrency(
                id = id.toString().toInt(),
                name = name.toString(),
                symbol = symbol.toString(),
                price = currency["price"].toString(),
                volume24h =currency["volume_24h"].toString(),
                volumeChange24h = currency["volume_change_24h"].toString(),
                percentageChange1h = currency["percent_change_1h"].toString(),
                percentageChange24h =  (Math.round((currency["percent_change_24h"].toString()).toDouble()) * 100/ 10000.0).toString(),
                percentageChange7d = currency["percent_change_7d"].toString(),
                percentageChange30d = currency["percent_change_30d"].toString(),
                percentageChange60d = currency["percent_change_60d"].toString(),
                percentageChange90d = currency["percent_change_90d"].toString(),
                marketCap = currency["market_cap"].toString() ,
                fullyDilutedMarketCap = currency["fully_diluted_market_cap"].toString()
            )
            )
        }

        return top10CryptoPrices

    }
}