package com.example.portfolio

import com.example.cryptodata.GetAllCryptoPrices
import com.example.database.CrudOperations
import online.pasaka.model.user.portfolio.LiveCryptoPrice
import online.pasaka.model.user.portfolio.LivePortfolio
import java.text.DecimalFormat

object GetUserPortfolio{
    suspend fun getUserPortfolio(email: String): LivePortfolio?{
        val userPortfolio = CrudOperations.getUserPortfolio(email)
        val decimalFormat = DecimalFormat("#.##")
        var total = 0.0
        val cryptos = GetAllCryptoPrices().getAllCryptoMetadata()
        val portfolio = mutableListOf<LiveCryptoPrice>()
        userPortfolio?.assets?.forEach {
                coin->
            run {
                cryptos?.forEach {
                    if (coin.symbol  == it.symbol.replace(regex = Regex("\""),"")){
                        println(coin.symbol)
                        println("amount = ${coin.amount}, price = ${it.price}")
                        total += it.price.toString().toDouble() * coin.amount
                        println(total)
                        portfolio.add(
                            LiveCryptoPrice(
                                symbol = coin.symbol,
                                name = coin.name,
                                amount = coin.amount,
                                marketPrice = decimalFormat.format(it.price.toString().toDouble() * coin.amount).toDouble()
                            )
                        )
                    }
                }
            }
        }
        val fullPortfolio = LivePortfolio(
            balance = decimalFormat.format(total).toDouble(),
            assets = portfolio
        )

        return fullPortfolio
    }

}