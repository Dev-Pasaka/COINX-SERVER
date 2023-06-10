package online.pasaka.routes

import online.pasaka.model.user.portfolio.LivePortfolio
import com.example.portfolio.GetUserPortfolio
import com.example.responses.Portfolio
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*


fun Route.getUserPortfolio(){
    authenticate("auth-jwt") {
        get("/getUserPortfolio") {
                val emailParams = call.parameters["email"]
                if (emailParams != null) {

                    val result: LivePortfolio? = try {
                        GetUserPortfolio.getUserPortfolio(emailParams)
                    } catch (e: Exception) {
                        null
                    }

                    if (result != null) call.respond(result) else call.respond(

                        status = HttpStatusCode.OK,
                        message = Portfolio(
                            message = "failed to get user:$emailParams portfolio",
                            status = false
                        )

                    )

                } else {
                    call.respond(

                        status = HttpStatusCode.OK,
                        message = Portfolio(
                            message = "Wrong parameters",
                            status = false
                        )

                    )
                }
            }
    }
}