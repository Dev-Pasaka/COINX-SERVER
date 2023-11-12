import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

fun getTimeAgo(timestamp: String): String {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")

    val currentTime = System.currentTimeMillis()
    val dateTime = dateFormat.parse(timestamp)?.time ?: return "Invalid timestamp"

    val timeDifference = currentTime - dateTime
    val seconds = timeDifference / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    val weeks = days / 7

    return when {
        weeks >= 4 -> "more than 4 weeks ago"
        weeks >= 1 -> "${weeks.toInt()} week${if (weeks.toInt() > 1) "s" else ""} ago"
        days >= 1 -> "${days.toInt()} day${if (days.toInt() > 1) "s" else ""} ago"
        hours >= 1 -> "${hours.toInt()} hour${if (hours.toInt() > 1) "s" else ""} ago"
        minutes >= 1 -> "${minutes.toInt()} minute${if (minutes.toInt() > 1) "s" else ""} ago"
        else -> "just now"
    }
}

fun main() {
    val timestamp = "2023-11-09 12:15:47"
    val timeAgo = getTimeAgo(timestamp)
    println(timeAgo)
}
