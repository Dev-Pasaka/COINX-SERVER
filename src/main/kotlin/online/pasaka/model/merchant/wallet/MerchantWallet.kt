package online.pasaka.model.merchant.wallet

import online.pasaka.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
data class MerchantWallet(
    var walletId :String = "",
    val assets:List<CryptoCoin> = listOf()

)

