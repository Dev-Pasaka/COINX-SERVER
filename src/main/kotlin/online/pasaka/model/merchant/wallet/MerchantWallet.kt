package online.pasaka.model.wallet

import online.pasaka.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable

@Serializable
data class Wallet(
    var walletId :String = "",
    val assets:List<CryptoCoin> = listOf()

)

