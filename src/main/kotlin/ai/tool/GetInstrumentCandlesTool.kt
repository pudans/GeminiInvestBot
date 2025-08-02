@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.ai.tool

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.*
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.models.CandlesRequest
import ru.pudans.investrobot.repository.MarketDataRepository
import kotlin.time.ExperimentalTime

class GetInstrumentCandlesTool(
    private val marketDataRepository: MarketDataRepository
) : GeminiToolExecutor {

    override val name: String = "GetInstrumentCandlesTool"
    override val declaration: Declaration = Declaration(
        name = name,
        description = "Retrieves historical price candles (OHLCV data) for a financial instrument over a specified time period and interval. Returns candlestick data including open, high, low, close prices and volume.",
        parameters = Parameters(
            type = Type.OBJECT,
            properties = mapOf(
                "instrumentId" to Property(
                    type = Type.STRING,
                    description = "Unique identifier of the financial instrument (UID)"
                ),
                "startTime" to Property(
                    type = Type.INTEGER,
                    description = "Start timestamp for the candle data period (Unix epoch seconds)"
                ),
                "endTime" to Property(
                    type = Type.INTEGER,
                    description = "End timestamp for the candle data period (Unix epoch seconds)"
                ),
                "interval" to Property(
                    type = Type.STRING,
                    enum = CandleInterval.entries.map { it.name },
                    description = "Time interval for candles"
                )
            ),
            required = listOf(
                "instrumentId",
                "startTime",
                "endTime",
                "interval"
            )
        )
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        val request = Json.decodeFromJsonElement<CandlesRequest>(args.args!!)
        println("$name request: $request")

        val response = marketDataRepository.getCandles(request).getOrThrow()
        println("$name response: $response")

        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = JsonObject(
                content = mapOf("result" to Json.encodeToJsonElement(response))
            )
        )
    }
}