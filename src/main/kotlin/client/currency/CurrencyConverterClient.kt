package client.currency

import client.currency.domain.CurrencyRequest
import client.currency.domain.CurrencyResponse
import exception.TelegramBusinessException
import exception.TelegramError
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.annotation.Property
import org.koin.core.annotation.Single

@Single
class CurrencyConverterClient(
    @Property("CURRENCY_API_KEY")
    private val apiKey: String,
    @Property("CURRENCY_USER_ID")
    private val userId: String,
    @Property("PROFILE")
    private val profile: String
) {

    val client = HttpClient(CIO) {
        expectSuccess = true

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        defaultRequest {
            url {
                protocol = URLProtocol.HTTPS
                host = "irates.io"
                path("api", "v1/")
                parameters.append("api_key", apiKey)
                parameters.append("user_id", userId)
            }
            contentType(ContentType.Application.Json)
        }
    }

    suspend fun createConversion(): CurrencyResponse? {
        return runCatching {
            client.post("conversions") {
                setBody(CurrencyRequest.generate(!profile.equals("prod", ignoreCase = true)))
            }.body<CurrencyResponse>()
                .also {
                    println("Осталось ${it.availableRequestsLeft} запросов к апи")
                }
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()
    }

    suspend fun getConversion(id: String): CurrencyResponse {
        return client.get("conversions") {
            url {
                appendPathSegments(id)
            }
        }.body<CurrencyResponse>()
            .also {
                println("Осталось ${it.availableRequestsLeft} запросов к апи")
            }
    }
}