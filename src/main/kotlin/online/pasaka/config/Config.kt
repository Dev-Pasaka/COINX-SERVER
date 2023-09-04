package com.example.config

import com.typesafe.config.ConfigFactory
import io.ktor.server.config.*

object Config {
    val load = HoconApplicationConfig( ConfigFactory.load())
}

fun main(){
    val config = Config.load
    val url = config.property("KAFKA_CLIENT_CERT").getString()
    println(url)
}