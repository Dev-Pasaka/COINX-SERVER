package online.pasaka.model.merchant.wallet

import online.pasaka.model.wallet.crypto.CryptoCoin
import kotlinx.serialization.Serializable
import java.util.Currency

@Serializable
data class MerchantWallet(
    var walletId :String = "",
    var merchantFloat:Double = 0.0,
    val currency: String = Currency.getInstance("USD").toString(),
    val assets:List<CryptoCoin> = listOf()

)

