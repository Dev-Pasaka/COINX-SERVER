package online.pasaka.module.resource.routes.User

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import online.pasaka.infrastructure.config.JWTConfig
import online.pasaka.infrastructure.database.DatabaseConnection
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.litote.kmongo.*
import org.mindrot.jbcrypt.BCrypt
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.domain.repository.cache.RedisRepositoryImpl
import online.pasaka.domain.responses.*
import java.util.*
import online.pasaka.domain.utils.Utils
import online.pasaka.domain.model.user.*
import online.pasaka.domain.model.user.portfolio.LivePortfolio
import online.pasaka.domain.repository.database.users.UserRepositoryImpl
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

fun Route.userRegistration() {

    post("/registerUser") {

        coroutineScope {

            val db = DatabaseConnection.database
            val userData = call.receive<online.pasaka.domain.model.user.UserRegistration>()
            val hashedPassword = BCrypt.hashpw(userData.password, BCrypt.gensalt())
            val userCollection = db.getCollection<online.pasaka.domain.model.user.User>("user")
            val doesEmailExists =
                async { userCollection.findOne(online.pasaka.domain.model.user.User::email eq userData.email) }

            if (doesEmailExists.await() == null) {

                val userRegistration = async {

                    UserRepositoryImpl().createUser(
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

                    UserRepositoryImpl().createWallet(

                        online.pasaka.domain.model.wallet.Wallet(
                            walletId = userData.email,
                            assets = listOf(

                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                    symbol = "USDT",
                                    name = "Tether",
                                    amount = 0.0
                                ),
                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
                                    symbol = "ETH",
                                    name = "Ethereum",
                                    amount = 0.0
                                ),
                                online.pasaka.domain.model.wallet.crypto.CryptoCoin(
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

                val getUserdataInRedis = async(Dispatchers.IO) { RedisRepositoryImpl().getData(key = "/getUserData$email") }.await()
                if (getUserdataInRedis != null){
                    val resultJson = getUserdataInRedis.removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    val resultObj = Json.decodeFromString<UserData>(resultJson)
                    call.respond(status = HttpStatusCode.OK,message = resultObj)
                }

                val userdata: User? = try {
                    async { UserRepositoryImpl().getUserData(email) }.await()
                } catch (e: Exception) {
                    null
                }
                if (userdata == null) call.respond(
                    UserDataResponse(
                        message = "Failed to fetch data for user $email",
                        status = false
                    )
                )
                else {
                    val user = UserData(
                        id = userdata.id,
                        fullName = userdata.fullName,
                        username = userdata.username,
                        phoneNumber = userdata.phoneNumber,
                        email = userdata.email,
                        country = userdata.country
                    )
                    launch(Dispatchers.IO) { RedisRepositoryImpl().setData(key = "/getUserData$email", user) }
                    call.respond(status = HttpStatusCode.OK, message = user)
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

                val getUserPortfolioInRedis = async(Dispatchers.IO) {
                    RedisRepositoryImpl().getData(key = "/getUserPortfolio$email")
                }.await()

                if (getUserPortfolioInRedis != null){
                    val formatResultJson  = getUserPortfolioInRedis.removePrefix("\"")
                        .removeSuffix("\"")
                        .replace("\\", "")
                    val resultObj = Json.decodeFromString<UserPortfolio>(formatResultJson)

                    call.respond(status = HttpStatusCode.OK,message = resultObj)
                }

                val result: LivePortfolio? = try {
                    UserRepositoryImpl().liveUserPortfolio(email)
                } catch (e: Exception) {
                    null
                }

                if (result != null) {
                    launch(Dispatchers.IO) { RedisRepositoryImpl().setData(key = "/getUserPortfolio$email", Json.encodeToString<LivePortfolio>(result)) }
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
                    UserRepositoryImpl().fetchUserCredentials(userCredentials.email)
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
                UserRepositoryImpl().checkIfPhoneExists(
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

                UserRepositoryImpl().checkIfPhoneExists(
                    phoneNumber = updatePassword.phoneNumber
                )

            }.await()

            if (verifyPhoneNumber != null) {

                val result = async {

                    UserRepositoryImpl().updatePassword(
                        email = verifyPhoneNumber.phoneNumber,
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

                    UserRepositoryImpl().deleteAccount(email = email)

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
