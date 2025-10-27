@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.signal.macd

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
 * Configuration for MACD Signal Generator
 */
data class MACDConfig(
    val fastLength: Int = 12,
    val slowLength: Int = 26,
    val signalLength: Int = 9,
    val stopLossPercent: Double = 0.025, // 2.5%
    val targetPercent: Double = 0.05,    // 5%
    val strongMomentumThreshold: Double = 0.1 // 10% of current MACD value
)

/**
 * Signal generator based on MACD (Moving Average Convergence Divergence) indicator
 * MACD crossovers and momentum changes indicate trend shifts
 */
class MACDSignalGenerator(
    private val config: MACDConfig = MACDConfig()
) : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "MACD"
    override val priority: Int = 3

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeMACDSignal(context) }

    private suspend fun analyzeMACDSignal(context: SignalContext): GeneratedSignal {
//        // Check for sufficient data
//        val hourlyCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]
//        if (hourlyCandles == null || hourlyCandles.size < 30) {
//            error("Insufficient data for MACD analysis (need at least 30 hourly candles)")
//        }

        // Get MACD data from technical analysis API
        val macdData = marketDataRepository.getTechAnalysis(
            request = TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_MACD,
                instrumentUid = context.instrument.uid,
                from = currentTime.minus(72.hours).epochSeconds,
                to = currentTime.epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                smoothingFastLength = config.fastLength,
                smoothingSlowLength = config.slowLength,
                smoothingSignal = config.signalLength
            )
        ).getOrThrow()

        if (macdData.size < 3) {
            error("Analysis data is not sufficient!")
        }

        return analyzeMACDData(macdData, context)
    }

    private fun analyzeMACDData(macdData: List<TechnicalIndicator>, context: SignalContext): GeneratedSignal {
        // Extract MACD values - assuming the API returns MACD line values
        val macdValues = macdData.map { it.price }

        // For real implementation, you'd need separate calls for MACD line, signal line, and histogram
        // This is a simplified version using just the MACD line

        val current = macdValues.last()
        val previous = if (macdValues.size >= 2) macdValues[macdValues.size - 2] else current
        val momentum = current - previous

        val (result, confidence, reasoning) = when {
            // Strong bullish momentum
            current > 0 && momentum > 0 && abs(momentum) > abs(current) * config.strongMomentumThreshold -> Triple(
                SignalResult.BUY,
                SignalConfidence.HIGH,
                "MACD bullish crossover with strong momentum (+${String.format("%.4f", momentum)})"
            )

            // Moderate bullish signal
            current > 0 && momentum > 0 -> Triple(
                SignalResult.BUY,
                SignalConfidence.MEDIUM,
                "MACD above zero with positive momentum (+${String.format("%.4f", momentum)})"
            )

            // Strong bearish momentum
            current < 0 && momentum < 0 && abs(momentum) > abs(current) * config.strongMomentumThreshold -> Triple(
                SignalResult.SELL,
                SignalConfidence.HIGH,
                "MACD bearish crossover with strong momentum (${String.format("%.4f", momentum)})"
            )

            // Moderate bearish signal
            current < 0 && momentum < 0 -> Triple(
                SignalResult.SELL,
                SignalConfidence.MEDIUM,
                "MACD below zero with negative momentum (${String.format("%.4f", momentum)})"
            )

            // Weak signals
            current > 0 && momentum <= 0 -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "MACD above zero but losing momentum (${String.format("%.4f", momentum)})"
            )

            current < 0 && momentum >= 0 -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "MACD below zero but gaining momentum (+${String.format("%.4f", momentum)})"
            )

            else -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "MACD neutral signal"
            )
        }

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
            probability = calculateMACDProbability(current, momentum, result),
            riskRewardRatio = if (targetPrice != null && stopLossPrice != null) {
                abs(targetPrice - currentPrice) / abs(currentPrice - stopLossPrice)
            } else null
        )
    }

    private fun calculateMACDProbability(macd: Double, momentum: Double, result: SignalResult): Double {
        return when (result) {
            SignalResult.BUY -> when {
                macd > 0 && momentum > 0 -> 0.80
                macd > 0 || momentum > 0 -> 0.65
                else -> 0.50
            }

            SignalResult.SELL -> when {
                macd < 0 && momentum < 0 -> 0.80
                macd < 0 || momentum < 0 -> 0.65
                else -> 0.50
            }

            else -> 0.50
        }
    }
}