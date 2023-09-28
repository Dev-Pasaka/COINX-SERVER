package online.pasaka.rates

object CreateCryptoAdsRate {
    const val buyOrderRate = 0.05

    fun withdrawalAmount(cryptoAmount:Double, rate:Double = 20.0): Double {

        val transactionCost = (rate/100)*cryptoAmount
        return cryptoAmount+transactionCost

    }
    fun swapAmount(amountInUSD:Double, rate: Double = 1.0):Double{
        val swapCost = (rate/100) * amountInUSD
        return swapCost
    }

    val maximumCreatBuyAdMargin = 0.05
}