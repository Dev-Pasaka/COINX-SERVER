package online.pasaka.rates

object CreateCryptoAdsRate {
    const val buyOrderRate = 0.05

    fun withdrawalAmount(cryptoAmount:Double, rate:Double = 20.0): Double {

        val transactionCost = (rate/100)*cryptoAmount
        return cryptoAmount+transactionCost

    }
    fun swapAmount(amountInUSD: Double, rate: Double = 5.0): Double {
        return (rate / 100) * amountInUSD
    }

    const val maximumCreateBuyAdMargin = 0.5
}