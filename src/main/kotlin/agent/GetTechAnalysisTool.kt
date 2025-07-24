package ru.pudans.investrobot.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolParameterDescriptor
import ai.koog.agents.core.tools.ToolParameterType
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import ru.pudans.investrobot.models.IndicatorInterval
import ru.pudans.investrobot.models.IndicatorType
import ru.pudans.investrobot.models.TechAnalysisRequest
import ru.pudans.investrobot.models.TechnicalIndicator
import ru.pudans.investrobot.models.TypeOfPrice
import ru.pudans.investrobot.repository.MarketDataRepository

class GetTechAnalysisTool(
    val marketDataRepository: MarketDataRepository
) : Tool<GetTechAnalysisTool.Args, GetTechAnalysisTool.Result>() {

    @Serializable
    data class Args(
        val indicatorType: IndicatorType,
        val instrumentUid: String,
        val from: Long,
        val to: Long,
        val interval: IndicatorInterval,
        val typeOfPrice: TypeOfPrice,
        val length: Int?,
        val deviation: Double?,
        val smoothingFastLength: Int?,
        val smoothingSlowLength: Int?,
        val smoothingSignal: Int?
    ) : ToolArgs

    @Serializable
    @JvmInline
    value class Result(
        val result: List<TechnicalIndicator>
    ) : ToolResult {
        override fun toStringDefault(): String {
            return result.toString()
        }
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "getTechAnalysisTool",
        description = "Performs technical analysis on financial instruments using various indicators (EMA, SMA, RSI, MACD, Bollinger Bands). Returns technical indicator values over the specified time period.",
        requiredParameters = listOf(
            ToolParameterDescriptor(
                name = "indicatorType",
                description = "Type of technical indicator to calculate (INDICATOR_TYPE_BB, INDICATOR_TYPE_EMA, INDICATOR_TYPE_RSI, INDICATOR_TYPE_MACD, INDICATOR_TYPE_SMA)",
                type = ToolParameterType.Enum(IndicatorType.entries),
            ),
            ToolParameterDescriptor(
                name = "instrumentUid",
                description = "Unique identifier of the financial instrument",
                type = ToolParameterType.String,
            ),
            ToolParameterDescriptor(
                name = "from",
                description = "Start timestamp for analysis period (Unix epoch seconds)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "to",
                description = "End timestamp for analysis period (Unix epoch seconds)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "interval",
                description = "Time interval for analysis (INDICATOR_INTERVAL_ONE_MINUTE, INDICATOR_INTERVAL_FIVE_MINUTES, INDICATOR_INTERVAL_FIFTEEN_MINUTES, INDICATOR_INTERVAL_ONE_HOUR, INDICATOR_INTERVAL_ONE_DAY)",
                type = ToolParameterType.String,
            ),
            ToolParameterDescriptor(
                name = "typeOfPrice",
                description = "Type of price to use for analysis (TYPE_OF_PRICE_CLOSE, TYPE_OF_PRICE_OPEN, TYPE_OF_PRICE_HIGH, TYPE_OF_PRICE_LOW, TYPE_OF_PRICE_AVG)",
                type = ToolParameterType.Enum(TypeOfPrice.entries),
            ),
            ToolParameterDescriptor(
                name = "length",
                description = "Length parameter for the indicator (optional, depends on indicator type)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "deviation",
                description = "Deviation parameter for Bollinger Bands (optional)",
                type = ToolParameterType.Float,
            ),
            ToolParameterDescriptor(
                name = "smoothingFastLength",
                description = "Fast smoothing length for MACD (optional)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "smoothingSlowLength",
                description = "Slow smoothing length for MACD (optional)",
                type = ToolParameterType.Integer,
            ),
            ToolParameterDescriptor(
                name = "smoothingSignal",
                description = "Signal smoothing parameter (optional)",
                type = ToolParameterType.Integer,
            ),
        )
    )

    override suspend fun execute(args: Args): Result {
        val request = TechAnalysisRequest(
            indicatorType = args.indicatorType,
            instrumentUid = args.instrumentUid,
            from = args.from,
            to = args.to,
            interval = args.interval,
            typeOfPrice = args.typeOfPrice,
            length = args.length,
            deviation = args.deviation,
            smoothingFastLength = args.smoothingFastLength,
            smoothingSlowLength = args.smoothingSlowLength,
            smoothingSignal = args.smoothingSignal
        )

        val analysisResult = marketDataRepository.getTechAnalysis(request).getOrThrow()
        return Result(analysisResult)
    }
}