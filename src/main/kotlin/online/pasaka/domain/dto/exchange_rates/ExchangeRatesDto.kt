package online.pasaka.domain.dto.exchange_rates

import kotlinx.serialization.Serializable

@Serializable
data class ExchangeRatesDto(
    val data: online.pasaka.domain.dto.exchange_rates.Data,
    val meta: online.pasaka.domain.dto.exchange_rates.Meta
)