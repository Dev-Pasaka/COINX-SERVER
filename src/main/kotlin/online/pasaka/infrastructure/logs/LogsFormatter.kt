package online.pasaka.infrastructure.logs


// ColorInfoConverter.kt


import ch.qos.logback.classic.pattern.ClassicConverter
import ch.qos.logback.classic.spi.ILoggingEvent

class LogsFormatter : ClassicConverter() {
    override fun convert(event: ILoggingEvent?): String {
        val msg = event?.message ?: ""
        return msg.replaceFirst("\\b200 OK: POST - /signIn in \\d+ms\\b", "\u001B[94m$0\u001B[0m")
    }
}
