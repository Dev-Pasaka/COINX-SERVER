package online.pasaka.domain.service.mailService.mailTemplate

import online.pasaka.domain.utils.Utils

fun sellOrderCancellationTemplate(
    title: String = "Seller has cancelled the order",
    iconUrl: String = "https://play-lh.googleusercontent.com/Yg7Lo7wiW-iLzcnaarj7nm5-hQjl7J9eTgEupxKzC79Vq8qyRgTBnxeWDap-yC8kHoE=w240-h480-rw",
    recipientName: String? = null,
    orderID: String? = null,
    cryptoName: String? = null,
    cryptoSymbol: String? = null,
    cryptoAmount: Double? = null,
    amountToSend: Double? = null,
) = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>$title</title>
        </head>
        <body>
            <table align="center" width="100%" style="max-width: 600px; padding: 20px; border: 1px solid #e0e0e0;">
                <tr>
                    <td align="center" bgcolor="#f8f8f8">
                        <img src=$iconUrl alt="Your Company Logo" style="max-width: 150px; padding: 20px;">
                    </td>
                </tr>
                <tr>
                    <td align="center" bgcolor="#ffffff" style="padding: 20px;">
                        <h1>$title</h1>
                        <p>Dear $recipientName,</p>
                        <p>Seller has cancelled the order. Kindly verify the order cancellation and report anything that seems suspicious</p>
                        <table style="width: 100%;">
                            <tr>
                                <td>Order ID:</td>
                                <td>$orderID</td>
                            </tr>
                            <tr>
                                <td>Order Timestamp:</td>
                                <td>${Utils.currentTimeStamp()}</td>
                            </tr>
                        </table>
                        <h2>Order Details</h2>
                        <table style="width: 100%; border: 1px solid #e0e0e0;">
                            <tr>
                                <th>Crypto name: $cryptoName ($cryptoSymbol)</th>
                                <th>Amount: $cryptoAmount $cryptoSymbol</th>
                                <th>Amount to receive: ${Utils.formatCurrency(amountToSend ?: 0.0, currencyCode = "KES")}</th>
                            </tr>
                        </table>
                        <p>If you have any questions or need further assistance, please feel free to contact our customer support team at support@coinx.co.ke.</p>
                        <p>Thank you for Trading Coinx. We look forward to serving you again in the future!</p>
                    </td>
                </tr>
                <tr>
                    <td align="center" bgcolor="#f8f8f8" style="padding: 20px;">
                        <p style="font-size: 12px; color: #999999;">
                            This is an auto email from Coinx. If you did not place this order, please contact us immediately.
                        </p>
                    </td>
                </tr>
            </table>
        </body>
        </html>

    ""${'"'}
""".trimIndent()