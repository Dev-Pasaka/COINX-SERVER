package online.pasaka.domain.repository.remote.forex_exchange_rates

import online.pasaka.domain.dto.exchange_rates.ExchangeRatesDto

interface GetExchangeRates {

    suspend fun getExchangeRates(fromCurrency:String = "USD", toCurrency:String):String?
}