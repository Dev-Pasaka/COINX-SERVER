package online.pasaka.module.plugins

import io.ktor.server.application.*
import io.ktor.server.plugins.callloging.*


fun Application.configureClientLogging(){
    install(CallLogging){
    }
}