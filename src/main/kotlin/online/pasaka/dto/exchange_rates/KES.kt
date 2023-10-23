package online.pasaka.dto.exchange_rates

import kotlinx.serialization.Serializable

@Serializable
data class KES(
    val code: String,
    val value: Double
)