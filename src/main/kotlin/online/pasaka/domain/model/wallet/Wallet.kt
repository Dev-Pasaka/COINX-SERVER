package online.pasaka.domain.model.wallet

import online.pasaka.domain.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable

@Serializable
data class Wallet(
    var walletId :String = "",
    val assets:List<online.pasaka.domain.model.wallet.crypto.CryptoCoin> = listOf()
)

