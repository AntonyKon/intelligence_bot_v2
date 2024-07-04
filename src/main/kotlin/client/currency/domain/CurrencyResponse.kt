package client.currency.domain

import config.serializers.LocalDateTimeUTCSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class CurrencyResponse(
    val id: String,
    @SerialName("rate_data")
    val rateData: List<RateData>,
    @SerialName("source_currency")
    val sourceCurrency: String,
    @SerialName("target_currency")
    val targetCurrency: String,
    @SerialName("result_amount")
    val resultAmount: Double,
    @SerialName("available_requests_left")
    val availableRequestsLeft: Int
)

@Serializable
data class RateData(
    @SerialName("date_time")
    @Serializable(with = LocalDateTimeUTCSerializer::class)
    val dateTime: LocalDateTime
)
