package online.pasaka.domain.responses

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.user.portfolio.LiveCryptoPrice
import online.pasaka.domain.model.wallet.crypto.CryptoCoin

@Serializable
data class UserPortfolio(
    val balance: Double = 0.0,
    val assets: List<LiveCryptoPrice> = listOf()
)