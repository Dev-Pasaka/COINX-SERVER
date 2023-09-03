package online.pasaka.utils

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object GetCurrentTime{
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
}