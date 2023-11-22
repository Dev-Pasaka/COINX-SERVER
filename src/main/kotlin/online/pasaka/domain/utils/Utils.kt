package online.pasaka.domain.utils

import com.google.gson.JsonParser
import kotlinx.serialization.json.Json
import online.pasaka.infrastructure.config.Redis
import online.pasaka.domain.dto.exchange_rates.ExchangeRatesDto
import redis.clients.jedis.Jedis
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object Utils{
    fun currentTimeStamp():String{
        // Get the current timestamp in milliseconds
        val currentTimestamp = System.currentTimeMillis()


        // Convert the timestamp to a LocalDateTime object
        val instant = Instant.ofEpochMilli(currentTimestamp)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

        // Define the desired date-time format
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Format the LocalDateTime object into a human-readable string
        return localDateTime.format(formatter)
    }

    fun currentTimeStampPlus(durationInMills:Int):String{
        // Get the current timestamp in milliseconds
        val currentTimestamp = System.currentTimeMillis()

        // Add 15 minutes to the current timestamp
        val timestampPlus15Minutes = currentTimestamp + durationInMills // 15 minutes in milliseconds

        // Convert the new timestamp to a LocalDateTime object
        val instant = Instant.ofEpochMilli(timestampPlus15Minutes)
        val localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime()

        // Define the desired date-time format
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        // Format the LocalDateTime object into a human-readable string
        return localDateTime.format(formatter)
    }

    fun formatCurrency(amount: Double = 0.0, currencyCode: String = "USD"): String {
        val currency = Currency.getInstance(currencyCode)
        val currencyFormatter = NumberFormat.getCurrencyInstance()
        currencyFormatter.currency = currency
        return currencyFormatter.format(amount)
    }
    fun getForexPriceKES():Double {
        val redis  = Jedis(Redis.REDIS_CLOUD_URL)
        val result = redis.get(Redis.FOREIGN_EXCHANGE_IN_KES_KEY)
        val data = Json.decodeFromString(online.pasaka.domain.dto.exchange_rates.ExchangeRatesDto.serializer(), result)
        return data.data.KES.value+0.15

    }


}