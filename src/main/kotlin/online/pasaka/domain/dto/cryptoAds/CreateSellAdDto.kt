package online.pasaka.domain.dto.cryptoAds

data class CreateSellAdDto(
    val email:String,
    val cryptoName:String,
    val cryptoSymbol:String,
    val totalAmount:Double,
    val margin:Double,
    val minLimit:Double,
    val maxLimit:Double,
)
