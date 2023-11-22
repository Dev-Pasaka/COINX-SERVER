package online.pasaka.domain.model.user.portfolio

import kotlinx.serialization.Serializable
import online.pasaka.domain.model.user.portfolio.LiveCryptoPrice

@Serializable

data class LivePortfolio(
    val balance: Double,
    val assets: List<online.pasaka.domain.model.user.portfolio.LiveCryptoPrice>
)
