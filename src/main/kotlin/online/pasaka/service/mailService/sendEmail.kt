package online.pasaka.service.mailService

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.config.Config
import java.util.Properties
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

suspend fun sendBuyOrderConfirmationEmail(
    title:String = "P2P Buy Order Confirmation",
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
    properties["mail.smtp.starttls.enable"] = "true" // Use TLS

    val session = Session.getDefaultInstance(properties, object : Authenticator() {
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
                message.subject = "Coinx P2P Buy Order Confirmation"
                message.description = "A p2p order has been made please login to the app, confirm and release crypto."


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
    //sendEmailUsingTLS()
    //p2pBuyOrderConfirmationEmail()
}
