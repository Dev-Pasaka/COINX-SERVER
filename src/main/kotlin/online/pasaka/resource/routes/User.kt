package online.pasaka.resource.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import online.pasaka.config.JWTConfig
import online.pasaka.database.DatabaseConnection
import online.pasaka.model.wallet.crypto.CryptoCoin
import online.pasaka.model.wallet.Wallet
import online.pasaka.responses.Registration
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.*
import org.mindrot.jbcrypt.BCrypt
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import online.pasaka.model.user.*
import online.pasaka.model.user.User
import online.pasaka.model.user.portfolio.LivePortfolio
import java.util.*
import online.pasaka.responses.*
import online.pasaka.service.UserServices
import online.pasaka.utils.Utils

fun Route.userRegistration() {

    post("/registerUser") {

        coroutineScope {

            val db = DatabaseConnection.database
            val userData = call.receive<UserRegistration>()
            val hashedPassword = BCrypt.hashpw(userData.password, BCrypt.gensalt())
            val userCollection = db.getCollection<User>("user")
            val doesEmailExists = async { userCollection.findOne(User::email eq userData.email) }

            if (doesEmailExists.await() == null) {

                val userRegistration = async {

                    UserServices.createUser(
                        userRegistration = User(
                            fullName = userData.fullName,
                            email = userData.email,
                            phoneNumber = userData.phoneNumber,
                            username = userData.username,
                            country = "Kenya",
                            password = hashedPassword,
                            createdAt = Utils.currentTimeStamp()
                        )
                    )

                }

                val walletCreation = async {

                    UserServices.createWallet(

                        Wallet(
                            walletId = userData.email,
                            assets = listOf(

                                CryptoCoin(
                                    symbol = "USDT",
                                    name = "Tether",
                                    amount = 0.0
                                ),
                                CryptoCoin(
                                    symbol = "ETH",
                                    name = "Ethereum",
                                    amount = 0.0
                                ),
                                CryptoCoin(
                                    symbol = "BTC",
                                    name = "Bitcoin",
                                    amount = 0.0
                                )

                            )
                        )
                    )
                }

                if (userRegistration.await() && walletCreation.await()) call.respond(

                    status = HttpStatusCode.OK,
                    message = Registration(
                        message = "Registration successful",
                        isRegistered = true
                    )

                )
                else call.respond(

                    status = HttpStatusCode.OK,
                    message = Registration(
                        message = "Registration Failed",
                        isRegistered = false
                    )

                )
            } else {
                call.respond(

                    status = HttpStatusCode.OK,
                    message = Registration(
                        message = "Email already exits",
                        isRegistered = false
                    )

                )
            }
        }
    }

}
fun Route.getUserData() {

    authenticate("auth-jwt") {

        get("/getUserData") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")

                val userdata = async {

                    try {

                        UserServices.getUserData(email)

                    } catch (e: ExceptionInInitializerError) {

                        null

                    }
                }.await()

                if (userdata != null) {

                    call.respond(

                        UserData(
                            id = userdata.id,
                            fullName = userdata.fullName,
                            username = userdata.username,
                            phoneNumber = userdata.phoneNumber,
                            email = userdata.email,
                            country = userdata.country
                        )

                    )

                } else {

                    call.respond(
                        UserDataResponse(
                            message = "Failed to fetch data for user $email",
                            status = false
                        )
                    )

                }
            }

        }
    }
}
fun Route.getUserPortfolio() {

    authenticate("auth-jwt") {

        get("/getUserPortfolio") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")

                val result: LivePortfolio? = try {

                    UserServices.liveUserPortfolio(email)

                } catch (e: Exception) {

                    null

                }

                if (result != null) {

                    call.application.environment.log.info("Hello from /api/v1!")
                    call.respond(result)

                } else {

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = Portfolio(
                            message = "failed to get user:$email portfolio",
                            status = false
                        )
                    )

                }
            }


        }
    }
}
fun Route.signIn() {

    post("/signIn") {

        coroutineScope {

            val userCredentials = call.receive<SignIn>()
            val userdata = async {

                try {

                    UserServices.fetchUserCredentials(userCredentials.email)

                } catch (e: Exception) {

                    null

                }

            }.await()

            val hashedPassword = userdata?.password ?: ""
            val email = userdata?.email ?: ""
            if (email.isBlank()) call.respond(SignInResponse())
            if (email == userCredentials.email && BCrypt.checkpw(userCredentials.password, hashedPassword)) {

                val token = JWT.create()
                    .withAudience(JWTConfig.audience)
                    .withIssuer(JWTConfig.issuer)
                    .withClaim("email", userCredentials.email)
                    .withExpiresAt(Date(System.currentTimeMillis() + (60000 * 3600)))
                    .sign(Algorithm.HMAC256(JWTConfig.secret))

                call.respond(hashMapOf("token" to token))

            } else call.respond(SignInResponse())
        }
    }
}
fun Route.verifyPhone() {

    post("/verifyPhoneNumber") {

        coroutineScope {

            val verifyPhone = call.receive<Phone>()
            val result = async {
                UserServices.checkIfPhoneExists(
                    phoneNumber = verifyPhone.phoneNumber
                )
            }.await()

            if (result != null) {

                call.respond(
                    status = HttpStatusCode.OK,
                    message = PhoneQuery(
                        status = true,
                        message = "Phone number verification successful"
                    )
                )

            } else {

                call.respond(
                    status = HttpStatusCode.OK,
                    message = PhoneQuery()
                )

            }
        }

    }


}
fun Route.updatePassword() {

    post("/updatePassword") {

        coroutineScope {

            val updatePassword = call.receive<UpdatePassword>()
            val hashedPassword = BCrypt.hashpw(updatePassword.newPassword, BCrypt.gensalt())
            val verifyPhoneNumber = async {

                UserServices.checkIfPhoneExists(
                    phoneNumber = updatePassword.phoneNumber
                )

            }.await()

            if (verifyPhoneNumber != null) {

                val result = async {

                    UserServices.updatePasswordByPhoneNumber(
                        phoneNumber = verifyPhoneNumber.phoneNumber,
                        newPassword = hashedPassword
                    )

                }.await()

                if (result != null) {

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = UpdatePasswordResponse(
                            status = true,
                            message = "Password updated successfully",
                        )
                    )

                } else {

                    call.respond(
                        status = HttpStatusCode.OK,
                        message = UpdatePasswordResponse(
                            status = false,
                            message = "Failed to update password ",
                        )
                    )

                }

            } else {

                call.respond(
                    status = HttpStatusCode.OK,
                    message = UpdatePasswordResponse(
                        message = "Password update failed please confirm your phone number"
                    )
                )

            }
        }
    }
}
fun Route.deleteAccount() {

    authenticate("auth-jwt") {

        get("/deleteAccount") {

            coroutineScope {

                val email =
                    call.principal<JWTPrincipal>()?.payload?.getClaim("email").toString().removeSurrounding("\"")

                val result = try {

                    UserServices.deleteAccount(email = email)

                } catch (e: Exception) {

                    false

                }

                when (result) {

                    true -> {

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = true,
                                message = """
                                    Account was successfully deleted and cannot be recovered.
                                    If you wish to use our services kindly register for a new account or contact
                                    out customer support.
                                """.trimIndent()
                            )
                        )

                    }

                    else -> {

                        call.respond(
                            status = HttpStatusCode.OK,
                            message = DefaultResponse(
                                status = false,
                                message = """
                                    Account deletion failed, this account might not exist 
                                    kindly contact customer support for more information.
                                """.trimIndent()
                            )
                        )

                    }
                }
            }
        }
    }
}
