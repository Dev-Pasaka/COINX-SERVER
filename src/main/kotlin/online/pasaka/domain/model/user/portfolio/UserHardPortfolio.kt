package online.pasaka.domain.model.user.portfolio

import online.pasaka.domain.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable

@Serializable
data class UserHardPortfolio (
    val balance: Double,
    val assets: List<online.pasaka.domain.model.wallet.crypto.CryptoCoin>
)
