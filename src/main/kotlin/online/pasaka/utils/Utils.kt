package online.pasaka.utils

import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

object Utils{
    fun currentTime():String{
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


    fun formatCurrency(amount: Double = 0.0, currencyCode: String = "USD"): String {
        val currency = Currency.getInstance(currencyCode)
        val currencyFormatter = NumberFormat.getCurrencyInstance()
        currencyFormatter.currency = currency
        return currencyFormatter.format(amount)
    }
}