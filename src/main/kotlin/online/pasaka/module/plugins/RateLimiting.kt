package online.pasaka.module.plugins

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.response.*
import io.ktor.util.*
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

fun Application.configureRateLimiting() {
    install(RateLimit) {

            global {
                rateLimiter(limit = 5, refillPeriod = 60.seconds)
            }


    }
}
