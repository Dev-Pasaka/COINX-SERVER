package online.pasaka.responses

import kotlinx.serialization.Serializable
import java.util.Currency
@Serializable
data class SwappingResults(
    val cryptoName:String? = null,
    val cryptoSymbol:String? = null,
    val cryptoAmount:Double? = null,
    val fiatAmount:String? = null,
    val message:DefaultResponse
)
