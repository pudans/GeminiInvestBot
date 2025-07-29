package ru.pudans.investrobot.ai.tool.ai.tool.ai.tool.ai.tool

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.ai.models.Parameters
import ru.pudans.investrobot.ai.models.Property
import ru.pudans.investrobot.ai.models.Type
import ru.pudans.investrobot.models.IndicatorInterval
import ru.pudans.investrobot.models.IndicatorType
import ru.pudans.investrobot.models.TechAnalysisRequest
import ru.pudans.investrobot.models.TypeOfPrice
import ru.pudans.investrobot.repository.MarketDataRepository

class GetTechAnalysisTool(
    private val marketDataRepository: MarketDataRepository
) : GeminiToolExecutor {

    override val name: String = "GetTechAnalysisTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Performs technical analysis on financial instruments using various indicators (EMA, SMA, RSI, MACD, Bollinger Bands). Returns technical indicator values over the specified time period.",
        parameters = Parameters(
            type = Type.OBJECT,
            properties = mapOf(
                "indicatorType" to Property(
                    type = Type.STRING,
                    enum = IndicatorType.entries.map { it.name },
                    description = "Type of technical indicator to calculate"
                ),
                "instrumentUid" to Property(
                    type = Type.STRING,
                    description = "Unique identifier of the financial instrument"
                ),
                "from" to Property(
                    type = Type.INTEGER,
                    description = "Start timestamp for analysis period (Unix epoch seconds)"
                ),
                "to" to Property(
                    type = Type.INTEGER,
                    description = "End timestamp for analysis period (Unix epoch seconds)"
                ),
                "interval" to Property(
                    type = Type.STRING,
                    enum = IndicatorInterval.entries.map { it.name },
                    description = "Time interval for analysis"
                ),
                "typeOfPrice" to Property(
                    type = Type.STRING,
                    enum = TypeOfPrice.entries.map { it.name },
                    description = "Type of price to use for analysis"
                ),
                "length" to Property(
                    type = Type.INTEGER,
                    description = "Length parameter for the indicator (optional, required for INDICATOR_TYPE_RSI)"
                ),
                "deviation" to Property(
                    type = Type.NUMBER,
                    description = "Deviation parameter for Bollinger Bands (optional)"
                ),
                "smoothingFastLength" to Property(
                    type = Type.NUMBER,
                    description = "Fast smoothing length for MACD (optional)"
                ),
                "smoothingSlowLength" to Property(
                    type = Type.NUMBER,
                    description = "Slow smoothing length for MACD (optional)"
                ),
                "smoothingSignal" to Property(
                    type = Type.NUMBER,
                    description = "Signal smoothing parameter (optional)"
                )
            ),
            required = listOf(
                "indicatorType",
                "instrumentUid",
                "from",
                "to",
                "interval",
                "typeOfPrice"
            )
        )
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        val request = Json.decodeFromJsonElement<TechAnalysisRequest>(args.args!!)
        println("$name request: $request")

        val response = marketDataRepository.getTechAnalysis(request).getOrThrow()
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