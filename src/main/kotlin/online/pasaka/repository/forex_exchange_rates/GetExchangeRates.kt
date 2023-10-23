package online.pasaka.repository.forex_exchange_rates

import online.pasaka.dto.exchange_rates.ExchangeRatesDto

interface GetExchangeRates {

    suspend fun getExchangeRates(fromCurrency:String = "USD", toCurrency:String):String?
}