package online.pasaka.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

object Config {
    val load = HoconApplicationConfig( ConfigFactory.load())
}