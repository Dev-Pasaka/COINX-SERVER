package online.pasaka.domain.model.merchant.wallet

import online.pasaka.domain.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
data class MerchantWallet(
    var walletId :String = "",
    val assets:List<online.pasaka.domain.model.wallet.crypto.CryptoCoin> = listOf()

)

