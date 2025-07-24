@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import ru.pudans.investrobot.models.Candle
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.repository.MarketDataRepository
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class GetInstrumentCandles(
    val marketDataRepository: MarketDataRepository
) : Tool<GetInstrumentCandles.Args, GetInstrumentCandles.Result>() {

    @Serializable
    data class Args(
        val instrumentId: String,
        val startTime: Long,
        val endTime: Long,
        val interval: CandleInterval
    ) : ToolArgs

    @Serializable
    @JvmInline
    value class Result(
        val candles: List<Candle>
    ) : ToolResult {
        override fun toStringDefault(): String {
            return "Retrieved ${candles.size} candles:\n${
                candles.joinToString("\n") {
                    "Time: ${it.time}, Open: ${it.open}, High: ${it.high}, Low: ${it.low}, Close: ${it.close}, Volume: ${it.volume}"
                }
            }"
        }
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "GetInstrumentCandles",
        description = "Retrieves historical price candles (OHLCV data) for a financial instrument over a specified time period and interval. Returns candlestick data including open, high, low, close prices and volume.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "instrumentId",
                description = "Unique identifier of the financial instrument (UID)",
                type = ToolParameterType.String,
            ),
            ToolParameterDescriptor(
                name = "startTime",
                description = "Start timestamp for the candle data period (Unix epoch seconds)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "endTime",
                description = "End timestamp for the candle data period (Unix epoch seconds)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "interval",
                description = "Time interval for candles (INTERVAL_1_MIN, INTERVAL_5_MIN, INTERVAL_15_MIN, INTERVAL_1_HOUR, INTERVAL_4_HOUR, INTERVAL_DAY, INTERVAL_WEEK, INTERVAL_MONTH)",
                type = ToolParameterType.Enum(CandleInterval.entries),
            ),
        )
    )

    override suspend fun execute(args: Args): Result {
        val startInstant = Instant.fromEpochSeconds(args.startTime)
        val endInstant = Instant.fromEpochSeconds(args.endTime)

        val candles = marketDataRepository.getCandles(
            instrumentId = args.instrumentId,
            startTime = startInstant,
            endTime = endInstant,
            interval = args.interval
        ).getOrThrow()

        return Result(candles)
    }
} 