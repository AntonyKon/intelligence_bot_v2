package client.currency.domain

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class CurrencyRequest(
    @SerialName("strategy_code")
    val strategyCode: String,
    @SerialName("source_currency")
    val sourceCurrency: String,
    @SerialName("target_currency")
    val targetCurrency: String,
    val amount: Int,
    @SerialName("source_currency_unit")
    val sourceCurrencyUnit: String,
    @SerialName("target_currency_unit")
    val targetCurrencyUnit: String,
    val rounding: Int,
    @SerialName("test_mode")
    val testMode: Boolean
) {
    companion object {
        fun generate(testMode: Boolean) = CurrencyRequest(
            strategyCode = "accurate_mir_payment_system_unified_system_core",
            sourceCurrency = "USD",
            targetCurrency = "RUB",
            amount = 1,
            sourceCurrencyUnit = "amount",
            targetCurrencyUnit = "amount",
            rounding = 4,
            testMode = testMode
        )
    }
}
