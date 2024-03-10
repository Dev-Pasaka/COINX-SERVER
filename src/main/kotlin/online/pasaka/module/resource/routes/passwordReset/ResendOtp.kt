package online.pasaka.module.resource.routes.passwordReset

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import online.pasaka.domain.responses.AfricasTalkingSmsResponse
import online.pasaka.domain.service.passwordResetService.PasswordResetService

fun Route.resendOtp() {
    get ("/resendOtp"){

        val phone = call.request.header("phone")
        if (phone.isNullOrBlank()){
            call.respond(
                status = HttpStatusCode.BadRequest,
                message = AfricasTalkingSmsResponse(
                    httpStatusCode = HttpStatusCode.BadRequest.value,
                    serverStatus = true,
                    errorMessage = "Wrong parameters"
                )
            )
        }

        val verifyOtpResult = PasswordResetService().resendOtp(phoneNumber = phone.toString())

        call.respond(
            status = HttpStatusCode.OK,
            message = verifyOtpResult
        )

    }
}