package online.pasaka.service.mailService

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import online.pasaka.config.Config
import online.pasaka.service.mailService.mailTemplate.*
import java.util.*
import javax.mail.*
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

suspend fun sellOrderEmail(
    title:String,
    recipientName:String,
    recipientEmail: String,
    orderID:String,
    cryptoName:String,
    cryptoSymbol:String,
    cryptoAmount:Double,
    amountToSend:Double
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
                            sellOrderConfirmationTemplate(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToSend = amountToSend
                            ),
                            "text/html"
                        )
                    }
                    "Funds Transferred" ->{
                        message.setContent(
                            merchantTransferredFundsTemplate(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToSend = amountToSend
                            ),
                            "text/html"
                        )
                    }
                    "Seller has cancelled the order" ->{
                        message.setContent(
                            sellOrderCancellationTemplate(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToSend = amountToSend
                            ),
                            "text/html"
                        )
                    }
                    "Deposit Was Successful" ->{
                        message.setContent(
                            sellerReleasedCryptoTemplate(
                                title = title,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                            ),
                            "text/html"
                        )
                    }
                    "Sell Order Has Expired" ->{
                        message.setContent(
                            expiredSellOrderTemplate(
                                title = title,
                                recipientName = recipientName,
                                orderID = orderID,
                                cryptoName = cryptoName,
                                cryptoSymbol = cryptoSymbol,
                                cryptoAmount = cryptoAmount,
                                amountToSend = amountToSend
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

suspend fun main(){
    sellOrderEmail(
        title = "Order Has Expired",
        orderID = "",
        recipientName="Pasaka",
        recipientEmail = "dev.pasaka@gmail.com",
        cryptoName = "",
        cryptoSymbol = "",
        cryptoAmount = 0.0,
        amountToSend = 0.0
    )
}