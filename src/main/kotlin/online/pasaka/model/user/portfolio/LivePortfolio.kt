package online.pasaka.model.user.portfolio

import kotlinx.serialization.Serializable
import online.pasaka.model.user.portfolio.LiveCryptoPrice

@Serializable

data class LivePortfolio(
    val balance: Double,
    val assets: List<LiveCryptoPrice>
)
