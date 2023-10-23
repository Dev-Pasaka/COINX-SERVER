package online.pasaka.repository.forex_exchange_rates

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement
import online.pasaka.config.Config
import online.pasaka.dto.exchange_rates.ExchangeRatesDto

class GetExchangeRateImpl:GetExchangeRates {
    override suspend fun getExchangeRates(fromCurrency: String, toCurrency: String): String? {

        return try {
            val config = Config.load
            val baseUrl = config.property("EXCHANGE_RATE_BASE_URL").getString()
            val apiKey = config.property("EXCHANGE_RATE_API_KEY").getString()
            val client = HttpClient(CIO)
            val request =  client.get("${baseUrl}v3/latest?apikey=${apiKey}=${toCurrency.uppercase()}")
            request.bodyAsText()
        }catch (e:Exception){
            e.printStackTrace()
            null
        }

    }
}
