package online.pasaka.module.resource.routes.passwordReset

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import online.pasaka.domain.dto.ResetPasswordDto
import online.pasaka.domain.service.passwordResetService.PasswordResetService

fun Route.resetPassword() {
    post("/resetPassword"){
        val newCredentials = call.receive<ResetPasswordDto>()

        val resetPasswordResult = PasswordResetService().resetPassword(
            phoneNumber = newCredentials.phoneNumber,
            otpCode = newCredentials.otpCode,
            newPassword =  newCredentials.otpCode,
        )

        call.respond(
            status = HttpStatusCode.OK,
            message = resetPasswordResult
        )

    }
}