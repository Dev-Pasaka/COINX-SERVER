package online.pasaka.routes

import com.example.aitservices.SendOtp
import online.pasaka.OtpCodeGenerator.OtpCodeGenerator
import online.pasaka.responses.SendOtpResponse
import com.example.database.CrudOperations
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import online.pasaka.model.user.Phone

fun Route.sendOtp(){
    post("/sendOtp"){
        val phoneNumber = call.receive<Phone>()
        println(phoneNumber)
        val verifyPhoneNumber = CrudOperations.checkIfPhoneExists(
            phoneNumber = phoneNumber.phoneNumber
        )
        println(phoneNumber)
        if (verifyPhoneNumber != null){
            val otpCode = OtpCodeGenerator.generateCode()
            val sendOtp = SendOtp.sendOtpViaSms(phone =verifyPhoneNumber.phoneNumber, otpCode = otpCode )
            if (sendOtp == true){
                call.respond(
                    status = HttpStatusCode.OK,
                    message = SendOtpResponse(
                        status = true,
                        message = "Code sent successfully",
                        otpCode = otpCode
                    )
                )
            }else call.respond(
                status = HttpStatusCode.OK,
                message = SendOtpResponse(
                    status = false,
                    message = "Code not sent",
                )
            )
        }
        else call.respond(
            status = HttpStatusCode.OK,
            message = SendOtpResponse(
                status = false,
                message = "Phone number does not exist"
            )
        )
    }
}