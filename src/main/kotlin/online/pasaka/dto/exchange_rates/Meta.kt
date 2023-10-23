package online.pasaka.dto.exchange_rates

import kotlinx.serialization.Serializable

@Serializable
data class Meta(
    val last_updated_at: String
)