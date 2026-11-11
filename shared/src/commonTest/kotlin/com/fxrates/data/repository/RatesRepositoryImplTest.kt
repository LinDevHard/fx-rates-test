package com.fxrates.data.repository

import com.fxrates.data.remote.FrankfurterApi
import com.fxrates.data.remote.FrankfurterBaseUrl
import com.fxrates.domain.model.RatePoint
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandler
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ServerResponseException
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.LocalDate
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

class RatesRepositoryImplTest : FunSpec({

    val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
    val clock = object : Clock {
        override fun now(): Instant = Instant.parse("2026-11-11T12:00:00Z")
    }

    fun repository(handler: MockRequestHandler): RatesRepositoryImpl {
        val client = HttpClient(MockEngine(handler)) {
            expectSuccess = true
            defaultRequest {
                url(FrankfurterBaseUrl)
            }
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
        }
        return RatesRepositoryImpl(api = FrankfurterApi(client), converter = RatesConverter(), clock = clock)
    }

    test("the history is requested for one currency over the last 30 days") {
        lateinit var request: HttpRequestData
        val repository = repository { incoming ->
            request = incoming
            respond(
                content = """
                    {"base":"USD","start_date":"2026-10-13","end_date":"2026-11-10","rates":{
                        "2026-11-10":{"EUR":0.89},
                        "2026-10-13":{"EUR":0.88}
                    }}
                """.trimIndent(),
                headers = jsonHeaders,
            )
        }

        val history = repository.getRateHistory(base = "USD", code = "EUR")

        request.url.encodedPath shouldEndWith "2026-10-13.."
        request.url.parameters["base"] shouldBe "USD"
        request.url.parameters["symbols"] shouldBe "EUR"
        history shouldBe listOf(
            RatePoint(LocalDate(2026, 10, 13), "0.88".toBigDecimal()),
            RatePoint(LocalDate(2026, 11, 10), "0.89".toBigDecimal()),
        )
    }

    test("a window without data gives an empty history") {
        val repository = repository {
            respond(content = """{"message":"not found"}""", status = HttpStatusCode.NotFound, headers = jsonHeaders)
        }

        repository.getRateHistory(base = "USD", code = "EUR").shouldBeEmpty()
    }

    test("a server failure is not hidden") {
        val repository = repository { respondError(HttpStatusCode.InternalServerError) }

        shouldThrow<ServerResponseException> { repository.getRateHistory(base = "USD", code = "EUR") }
    }

    test("a client error other than not found is not hidden") {
        val repository = repository {
            respond(
                content = """{"message":"bad request"}""",
                status = HttpStatusCode.BadRequest,
                headers = jsonHeaders,
            )
        }

        val failure = shouldThrow<ClientRequestException> { repository.getRateHistory(base = "USD", code = "EUR") }

        failure.response.status shouldBe HttpStatusCode.BadRequest
    }

})
