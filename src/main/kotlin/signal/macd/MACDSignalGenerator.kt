@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.signal.macd

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.*
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.signal.*
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Signal generator based on MACD (Moving Average Convergence Divergence) indicator
 * MACD crossovers and momentum changes indicate trend shifts
 */
class MACDSignalGenerator : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "MACD"
    override val priority: Int = 3

    private val fastLength = 12
    private val slowLength = 26
    private val signalLength = 9

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeMACDSignal(context) }

    private suspend fun analyzeMACDSignal(context: SignalContext): GeneratedSignal {
        // Check for sufficient data
        val hourlyCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]
        if (hourlyCandles == null || hourlyCandles.size < 30) {
            error("Insufficient data for MACD analysis (need at least 30 hourly candles)")
        }

        // Get MACD data from technical analysis API
        val macdData = marketDataRepository.getTechAnalysis(
            request = TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_MACD,
                instrumentUid = context.instrument.uid,
                from = Clock.System.now().minus(72.hours).epochSeconds,
                to = Clock.System.now().epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                smoothingFastLength = fastLength,
                smoothingSlowLength = slowLength,
                smoothingSignal = signalLength
            )
        ).getOrThrow()

        if (macdData.size < 3) {
            error("Analysis data is not sufficient!")
        }

        return analyzeMACDData(macdData, context)
    }

//    private fun calculateManualMACD(context: SignalContext, candles: List<ru.pudans.investrobot.models.Candle>): GeneratedSignal {
//        val prices = candles.map { it.close }
//
//        if (prices.size < slowLength + signalLength) {
//            return GeneratedSignal(
//                name = name,
//                result = SignalResult.WAIT,
//                confidence = SignalConfidence.LOW,
//                reasoning = "Insufficient price data for MACD calculation",
//                timeframe = CandleInterval.INTERVAL_1_HOUR
//            )
//        }
//
//        // Calculate EMAs
//        val fastEMA = calculateEMA(prices, fastLength)
//        val slowEMA = calculateEMA(prices, slowLength)
//
//        // Calculate MACD line (fast EMA - slow EMA)
//        val macdLine = fastEMA.zip(slowEMA) { fast, slow -> fast - slow }
//
//        // Calculate Signal line (EMA of MACD line)
//        val signalLine = calculateEMA(macdLine, signalLength)
//
//        // Calculate Histogram (MACD - Signal)
//        val histogram = macdLine.zip(signalLine) { macd, signal -> macd - signal }
//
//        return analyzeMACDValues(macdLine, signalLine, histogram, context)
//    }

//    private fun calculateEMA(prices: List<Double>, period: Int): List<Double> {
//        val multiplier = 2.0 / (period + 1)
//        val ema = mutableListOf<Double>()
//
//        // Start with SMA for first value
//        ema.add(prices.take(period).average())
//
//        for (i in period until prices.size) {
//            val value = (prices[i] * multiplier) + (ema.last() * (1 - multiplier))
//            ema.add(value)
//        }
//
//        return ema
//    }

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
            current > 0 && momentum > 0 && abs(momentum) > abs(current) * 0.1 -> Triple(
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
            current < 0 && momentum < 0 && abs(momentum) > abs(current) * 0.1 -> Triple(
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
        val stopLossDistance = currentPrice * 0.025 // 2.5% stop loss
        val targetDistance = currentPrice * 0.05   // 5% target

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

//    private fun analyzeMACDValues(
//        macdLine: List<Double>,
//        signalLine: List<Double>,
//        histogram: List<Double>,
//        context: SignalContext
//    ): GeneratedSignal {
//        val currentMACD = macdLine.last()
//        val currentSignal = signalLine.last()
//        val currentHistogram = histogram.last()
//
//        val previousMACD = if (macdLine.size >= 2) macdLine[macdLine.size - 2] else currentMACD
//        val previousSignal = if (signalLine.size >= 2) signalLine[signalLine.size - 2] else currentSignal
//        val previousHistogram = if (histogram.size >= 2) histogram[histogram.size - 2] else currentHistogram
//
//        // Detect crossovers
//        val bullishCrossover = previousMACD <= previousSignal && currentMACD > currentSignal
//        val bearishCrossover = previousMACD >= previousSignal && currentMACD < currentSignal
//
//        val (result, confidence, reasoning) = when {
//            bullishCrossover && currentHistogram > 0 -> Triple(
//                SignalResult.BUY,
//                SignalConfidence.VERY_HIGH,
//                "MACD bullish crossover confirmed with positive histogram"
//            )
//
//            bearishCrossover && currentHistogram < 0 -> Triple(
//                SignalResult.SELL,
//                SignalConfidence.VERY_HIGH,
//                "MACD bearish crossover confirmed with negative histogram"
//            )
//
//            currentMACD > currentSignal && currentHistogram > previousHistogram -> Triple(
//                SignalResult.BUY,
//                SignalConfidence.MEDIUM,
//                "MACD above signal line with increasing momentum"
//            )
//
//            currentMACD < currentSignal && currentHistogram < previousHistogram -> Triple(
//                SignalResult.SELL,
//                SignalConfidence.MEDIUM,
//                "MACD below signal line with decreasing momentum"
//            )
//
//            else -> Triple(
//                SignalResult.HOLD,
//                SignalConfidence.LOW,
//                "MACD showing mixed signals"
//            )
//        }
//
//        val currentPrice = context.currentPrice
//        val stopLossDistance = currentPrice * 0.025
//        val targetDistance = currentPrice * 0.05
//
//        val (targetPrice, stopLossPrice) = when (result) {
//            SignalResult.BUY -> Pair(currentPrice + targetDistance, currentPrice - stopLossDistance)
//            SignalResult.SELL -> Pair(currentPrice - targetDistance, currentPrice + stopLossDistance)
//            else -> Pair(null, null)
//        }
//
//        return GeneratedSignal(
//            name = name,
//            result = result,
//            confidence = confidence,
//            reasoning = reasoning,
//            entryPrice = currentPrice,
//            targetPrice = targetPrice,
//            stopLossPrice = stopLossPrice,
//            timeframe = CandleInterval.INTERVAL_1_HOUR,
//            probability = calculateMACDProbability(currentMACD, currentHistogram, result)
//        )
//    }

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