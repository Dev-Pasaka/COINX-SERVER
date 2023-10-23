package online.pasaka.scheduleTasks

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import online.pasaka.config.Redis
import online.pasaka.dto.exchange_rates.ExchangeRatesDto
import online.pasaka.repository.forex_exchange_rates.GetExchangeRateImpl
import redis.clients.jedis.Jedis
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

object CurrencyUpdater {
    @OptIn(DelicateCoroutinesApi::class)


    suspend fun updateCurrenciesInRedis() {
        val scheduler = Executors.newScheduledThreadPool(1)

        // Calculate the current time
        val currentTimeMillis = System.currentTimeMillis()

        // Set the desired times for running (e.g., at 9 AM and 5 PM)
        val desiredTimesMillis = listOf(
            calculateTimeMillis(6, 0),  // 9:00 AM
            calculateTimeMillis(18, 0) // 5:00 PM
        )

        for (desiredTimeMillis in desiredTimesMillis) {
            val initialDelayMillis = calculateInitialDelay(desiredTimeMillis, currentTimeMillis)
            scheduler.scheduleAtFixedRate(
                {
                    GlobalScope.launch {
                        updateToRedis()
                    }
                },
                initialDelayMillis,
                TimeUnit.HOURS.toMillis(12), // 12 hours period
                TimeUnit.MILLISECONDS
            )
        }
    }

    private fun calculateTimeMillis(hour: Int, minute: Int): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
        calendar.set(java.util.Calendar.MINUTE, minute)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun calculateInitialDelay(desiredTimeMillis: Long, currentTimeMillis: Long): Long {
        val currentDayMillis = TimeUnit.DAYS.toMillis(
            TimeUnit.MILLISECONDS.toDays(currentTimeMillis)
        )
        val nextDesiredTimeMillis = if (desiredTimeMillis <= currentTimeMillis) {
            // If the desired time has already passed today, schedule it for tomorrow
            desiredTimeMillis + TimeUnit.DAYS.toMillis(1)
        } else {
            desiredTimeMillis
        }
        return nextDesiredTimeMillis - currentDayMillis
    }


    suspend fun updateToRedis(){
        val jedis  = Jedis(Redis.REDIS_CLOUD_URL)
        val getRates = GetExchangeRateImpl().getExchangeRates(toCurrency = "KES")
        jedis.set(Redis.FOREIGN_EXCHANGE_IN_KES_KEY.toByteArray(),getRates.toString().toByteArray())
        println(jedis.get(Redis.FOREIGN_EXCHANGE_IN_KES_KEY))

    }
}
suspend fun main(){
    CurrencyUpdater.updateToRedis()
}
