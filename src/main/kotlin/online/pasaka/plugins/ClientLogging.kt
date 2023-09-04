package online.pasaka.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*


fun Application.configureClientLogging(){
    install(CallLogging){
    }
}