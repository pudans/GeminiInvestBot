@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.signal.rsi

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.*
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.signal.*
import ru.pudans.investrobot.tinkoff.currentTime
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Configuration for RSI Signal Generator
 */
data class RSIConfig(
    val rsiPeriod: Int = 14,
    val overboughtLevel: Double = 70.0,
    val oversoldLevel: Double = 30.0,
    val extremeOverboughtLevel: Double = 80.0,
    val extremeOversoldLevel: Double = 20.0,
    val neutralLevel: Double = 50.0,
    val momentumThreshold: Double = 5.0,
    val stopLossPercent: Double = 0.02, // 2%
    val targetPercent: Double = 0.04     // 4%
)

/**
 * Signal generator based on RSI (Relative Strength Index) indicator
 * RSI values above 70 indicate overbought conditions (potential SELL signal)
 * RSI values below 30 indicate oversold conditions (potential BUY signal)
 */
class RSISignalGenerator(
    private val config: RSIConfig = RSIConfig()
) : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "RSI"
    override val priority: Int = 2

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeRSI(context) }

    private suspend fun analyzeRSI(context: SignalContext): GeneratedSignal {
//        // Check if we have enough candle data
//        val hourlyCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]
//        if (hourlyCandles == null || hourlyCandles.size < config.rsiPeriod + 5) {
//            error("Insufficient data for RSI analysis (need at least ${config.rsiPeriod + 5} hourly candles)")
//        }

        // Get RSI data from technical analysis API
        val rsiData = marketDataRepository.getTechAnalysis(
            request = TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_RSI,
                instrumentUid = context.instrument.uid,
                from = currentTime.minus(48.hours).epochSeconds,
                to = currentTime.epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                length = config.rsiPeriod
            )
        ).getOrThrow()

        if (rsiData.isEmpty()) {
            error("Analysis data is empty!")
        }

        val currentRSI = rsiData.last().price
        val previousRSI = if (rsiData.size >= 2) rsiData[rsiData.size - 2].price else currentRSI
        val rsiTrend = currentRSI - previousRSI

        return analyzeRSIValue(currentRSI, rsiTrend, context)
    }

//    private fun calculateManualRSI(context: SignalContext, candles: List<ru.pudans.investrobot.models.Candle>): GeneratedSignal {
//        // Simple RSI calculation as fallback
//        val prices = candles.takeLast(rsiPeriod + 1).map { it.close }
//
//        if (prices.size < rsiPeriod + 1) {
//            return GeneratedSignal(
//                name = name,
//                result = SignalResult.WAIT,
//                confidence = SignalConfidence.LOW,
//                reasoning = "Insufficient price data for manual RSI calculation",
//                timeframe = CandleInterval.INTERVAL_1_HOUR
//            )
//        }
//
//        // Calculate price changes
//        val priceChanges = prices.zipWithNext { a, b -> b - a }
//
//        // Separate gains and losses
//        val gains = priceChanges.map { if (it > 0) it else 0.0 }
//        val losses = priceChanges.map { if (it < 0) -it else 0.0 }
//
//        // Calculate average gains and losses
//        val avgGain = gains.takeLast(rsiPeriod).average()
//        val avgLoss = losses.takeLast(rsiPeriod).average()
//
//        // Calculate RSI
//        val rsi = if (avgLoss == 0.0) {
//            100.0 // No losses = maximum RSI
//        } else {
//            val rs = avgGain / avgLoss
//            100.0 - (100.0 / (1.0 + rs))
//        }
//
//        return analyzeRSIValue(rsi, 0.0, context)
//    }

    private fun analyzeRSIValue(rsi: Double, rsiTrend: Double, context: SignalContext): GeneratedSignal {
        val (result, confidence, reasoning) = when {
            // Strong oversold conditions
            rsi <= config.extremeOversoldLevel -> Triple(
                SignalResult.BUY,
                SignalConfidence.VERY_HIGH,
                "RSI extremely oversold at ${rsi.toInt()}, strong buy signal"
            )

            // Moderate oversold conditions
            rsi <= config.oversoldLevel -> {
                val conf = if (rsiTrend > 0) SignalConfidence.HIGH else SignalConfidence.MEDIUM
                Triple(
                    SignalResult.BUY,
                    conf,
                    "RSI oversold at ${rsi.toInt()}" + if (rsiTrend > 0) ", showing reversal signs" else ""
                )
            }

            // Strong overbought conditions
            rsi >= config.extremeOverboughtLevel -> Triple(
                SignalResult.SELL,
                SignalConfidence.VERY_HIGH,
                "RSI extremely overbought at ${rsi.toInt()}, strong sell signal"
            )

            // Moderate overbought conditions
            rsi >= config.overboughtLevel -> {
                val conf = if (rsiTrend < 0) SignalConfidence.HIGH else SignalConfidence.MEDIUM
                Triple(
                    SignalResult.SELL,
                    conf,
                    "RSI overbought at ${rsi.toInt()}" + if (rsiTrend < 0) ", showing reversal signs" else ""
                )
            }

            // RSI in neutral zone but showing momentum
            rsi > config.neutralLevel && rsiTrend > config.momentumThreshold -> Triple(
                SignalResult.BUY,
                SignalConfidence.LOW,
                "RSI at ${rsi.toInt()} with bullish momentum (+${rsiTrend.toInt()})"
            )

            rsi < config.neutralLevel && rsiTrend < -config.momentumThreshold -> Triple(
                SignalResult.SELL,
                SignalConfidence.LOW,
                "RSI at ${rsi.toInt()} with bearish momentum (${rsiTrend.toInt()})"
            )

            // Neutral zone
            else -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "RSI neutral at ${rsi.toInt()}, no clear signal"
            )
        }

        // Calculate suggested stop loss and target based on RSI levels
        val currentPrice = context.currentPrice
        val stopLossDistance = currentPrice * config.stopLossPercent
        val targetDistance = currentPrice * config.targetPercent

        val (targetPrice, stopLossPrice) = when (result) {
            SignalResult.BUY -> Pair(
                currentPrice + targetDistance,
                currentPrice - stopLossDistance
            )

            SignalResult.SELL -> Pair(
                currentPrice - targetDistance,
                currentPrice + stopLossDistance
            )

            else -> Pair(null, null)
        }

        return GeneratedSignal(
            name = name,
            result = result,
            confidence = confidence,
            reasoning = reasoning,
            entryPrice = currentPrice,
            targetPrice = targetPrice,
            stopLossPrice = stopLossPrice,
            timeframe = CandleInterval.INTERVAL_1_HOUR,
            probability = calculateProbability(rsi, result),
            riskRewardRatio = if (targetPrice != null && stopLossPrice != null) {
                abs(targetPrice - currentPrice) / abs(currentPrice - stopLossPrice)
            } else null
        )
    }

    private fun calculateProbability(rsi: Double, result: SignalResult): Double {
        return when (result) {
            SignalResult.BUY -> when {
                rsi <= config.extremeOversoldLevel -> 0.85
                rsi <= config.oversoldLevel -> 0.75
                rsi <= 40 -> 0.60
                else -> 0.50
            }

            SignalResult.SELL -> when {
                rsi >= config.extremeOverboughtLevel -> 0.85
                rsi >= config.overboughtLevel -> 0.75
                rsi >= 60 -> 0.60
                else -> 0.50
            }

            else -> 0.50
        }
    }
}