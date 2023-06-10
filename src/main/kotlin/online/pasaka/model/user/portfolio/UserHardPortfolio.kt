package online.pasaka.model.user.portfolio

import online.pasaka.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable

@Serializable
data class UserHardPortfolio (
    val balance: Double,
    val assets: List<CryptoCoin>
)
