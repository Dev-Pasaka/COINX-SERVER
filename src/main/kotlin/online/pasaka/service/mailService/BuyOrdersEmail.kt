package online.pasaka.service.mailService

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.config.Config
import online.pasaka.service.mailService.mailTemplate.*
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

suspend fun buyOrderEmail(
    title:String,
    recipientName:String,
    recipientEmail: String,
    orderID:String,
    cryptoName:String,
    cryptoSymbol:String,
    cryptoAmount:Double,
    amountToReceive:Double
) {
    val config = Config.load
    val properties = Properties()
    properties["mail.smtp.host"] = config.property("SMTP_SERVER_ADDRESS").getString() // SMTP server address
    properties["mail.smtp.starttls.enable"] = "true"
    properties["mail.smtp.port"] = "587" // Port for sending emails (587 for TLS, 465 for SSL)
    properties["mail.smtp.auth"] = "true" // Enable authentication

    val session = Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(config.property("USERNAME").getString(), config.property("PASSWORD").getString())
        }
    })

    coroutineScope {
        launch {
            try {
                val message = MimeMessage(session)
                message.setFrom(InternetAddress(config.property("USERNAME").getString(), "Coinx"))
                message.setRecipient(Message.RecipientType.TO, InternetAddress(recipientEmail))
                message.subject = title

                when(title){
                   "P2P Order Confirmation" ->{
                       message.setContent(
                           buyOrderConfirmationTemplate(
                               title = title,
                               recipientName = recipientName,
                               orderID = orderID,
                               cryptoName = cryptoName,
                               cryptoSymbol = cryptoSymbol,
                               cryptoAmount = cryptoAmount,
                               amountToReceive = amountToReceive
                           ),
                           "text/html"
                       )
                   }
                    "Funds Transferred" ->{
                        message.setContent(
                            fundsTransferredTemplate(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToReceive = amountToReceive
                            ), "text/html"

                        )
                    }
                    "Buyer Has Cancelled The Order" ->{
                        message.setContent(
                            buyOrderCancellation(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToReceive = amountToReceive
                            ), "text/html"

                        )
                    }
                    "Buy Order has expired" ->{
                        message.setContent(
                            expiredOrderTemplate(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToReceive = amountToReceive
                            ), "text/html"

                        )
                    }
                    "Deposit Was Successful" ->{
                        message.setContent(
                            buyCryptoReleased(
                                title = title,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                            ), "text/html"

                        )
                    }
                }

                Transport.send(message)
                println("Email sent successfully.")
            } catch (e: MessagingException) {
                e.printStackTrace()
                println("Failed to send email.")
            }
        }
    }
}





suspend fun main() {
buyOrderEmail(
    title = "",
    orderID =  "",
    recipientName = "",
    recipientEmail = "dev.pasaka@gmail.com",
    cryptoName = "",
    cryptoSymbol = "",
    cryptoAmount = 0.0,
    amountToReceive = 0.0,
)
    /*val config = Config.load
    val properties = Properties()
    properties["mail.smtp.host"] = config.property("SMTP_SERVER_ADDRESS").getString() // SMTP server address
    properties["mail.smtp.starttls.enable"] = "true"
    properties["mail.smtp.port"] = "587" // Port for sending emails (587 for TLS, 465 for SSL)
    properties["mail.smtp.auth"] = "true" // Enable authentication
    val session = Session.getInstance(properties, object : Authenticator() {
        override fun getPasswordAuthentication(): PasswordAuthentication {
            return PasswordAuthentication(config.property("USERNAME").getString(), config.property("PASSWORD").getString())
        }
    })
    println(session.transport)*/
}
