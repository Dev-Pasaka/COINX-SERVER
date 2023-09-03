package online.pasaka.model.merchant.wallet.crypto

import kotlinx.serialization.Serializable

@Serializable
data class Cryptocurrency(

    val id: Int,
    val name: String,
    val symbol: String,
    val price: String?,
    val volume24h: String?,
    val volumeChange24h: String?,
    val percentageChange1h: String?,
    val percentageChange24h: String?,
    val percentageChange7d:  String?,
    val percentageChange30d: String?,
    val percentageChange60d: String?,
    val percentageChange90d: String?,
    val marketCap: String?,
    val fullyDilutedMarketCap: String?

)
