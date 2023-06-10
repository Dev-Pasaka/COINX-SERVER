package com.example.aitservices

import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*

object SendOtp {

    private val config = HoconApplicationConfig(ConfigFactory.load())

    suspend fun sendOtpViaSms(
            username:String = "SPX",
            phone:String,
            otpCode:String,
            message:String="Coinx\n" + "OTP CODE : "
        ): Boolean? {
            val client = HttpClient(CIO){
                install(ContentNegotiation){
                    json()
                }
            }

        return try {
            val request = client.post(config.property("africastalking_baseurl").getString()){
                header("apikey",config.property("africastalking_api_key").getString())
                setBody(FormDataContent(Parameters.build {
                    append("username", username)
                    append("to",phone)
                    append("message","$message $otpCode")
                }))
            }
            println(request.status.value)
            request.status.value == 201
        }catch (_:Exception){
            null
        }

    }

}

