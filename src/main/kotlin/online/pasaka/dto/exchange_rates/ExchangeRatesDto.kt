package online.pasaka.dto.exchange_rates

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRatesDto(
    val data: Data,
    val meta: Meta
)