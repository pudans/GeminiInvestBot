@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.signal.bb

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.*
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.signal.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Signal generator based on Bollinger Bands indicator
 * Bollinger Bands help identify overbought/oversold conditions and volatility
 * - Price touching upper band = potential sell signal (overbought)
 * - Price touching lower band = potential buy signal (oversold)
 * - Band squeeze = low volatility, potential breakout coming
 */
class BollingerBandsSignalGenerator : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "BollingerBands"
    override val priority: Int = 2

    private val period = 20
    private val standardDeviations = 2.0

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeBollingerBands(context) }

    private suspend fun analyzeBollingerBands(context: SignalContext): GeneratedSignal {
        // Check for sufficient data
        val hourlyCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]
        if (hourlyCandles == null || hourlyCandles.size < period + 5) {
            error("Insufficient data for Bollinger Bands analysis (need at least ${period + 5} hourly candles)")
        }

        // Try to get Bollinger Bands data from API
        val bollingerData = marketDataRepository.getTechAnalysis(
            request = TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_BB,
                instrumentUid = context.instrument.uid,
                from = Clock.System.now().minus(48.hours).epochSeconds,
                to = Clock.System.now().epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                length = period,
                deviation = standardDeviations
            )
        ).getOrThrow()

        if (bollingerData.isEmpty()) {
            error("Tech analysis is empty")
        }

        return analyzeBollingerData(bollingerData, context)
    }

//    private fun calculateManualBollingerBands(context: SignalContext, candles: List<ru.pudans.investrobot.models.Candle>): GeneratedSignal {
//        val prices = candles.map { it.close }
//
//        if (prices.size < period) {
//            return GeneratedSignal(
//                name = name,
//                result = SignalResult.WAIT,
//                confidence = SignalConfidence.LOW,
//                reasoning = "Insufficient price data for Bollinger Bands calculation",
//                timeframe = CandleInterval.INTERVAL_1_HOUR
//            )
//        }
//
//        // Calculate recent period for analysis
//        val recentPrices = prices.takeLast(period)
//        val currentPrice = context.currentPrice
//
//        // Calculate middle band (SMA)
//        val middleBand = recentPrices.average()
//
//        // Calculate standard deviation
//        val variance = recentPrices.map { (it - middleBand).pow(2) }.average()
//        val stdDev = sqrt(variance)
//
//        // Calculate upper and lower bands
//        val upperBand = middleBand + (standardDeviations * stdDev)
//        val lowerBand = middleBand - (standardDeviations * stdDev)
//
//        // Calculate band width (volatility indicator)
//        val bandWidth = (upperBand - lowerBand) / middleBand
//
//        // Calculate %B (position within bands)
//        val percentB = (currentPrice - lowerBand) / (upperBand - lowerBand)
//
//        return analyzeBollingerValues(currentPrice, upperBand, middleBand, lowerBand, percentB, bandWidth, context)
//    }

    private fun analyzeBollingerData(bollingerData: List<TechnicalIndicator>, context: SignalContext): GeneratedSignal {
        // Note: In real implementation, you'd need separate API calls for upper, middle, and lower bands
        // This is simplified assuming the API returns middle band values

        val middleBand = bollingerData.last().price
        val currentPrice = context.currentPrice

        // Estimate bands based on recent price action (simplified)
        val recentCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]?.takeLast(period)
        if (recentCandles == null || recentCandles.size < period) {
            error("Cannot calculate Bollinger Bands without sufficient recent data")
        }

        val recentPrices = recentCandles.map { it.close }
        val variance = recentPrices.map { (it - middleBand).pow(2) }.average()
        val stdDev = sqrt(variance)

        val upperBand = middleBand + (standardDeviations * stdDev)
        val lowerBand = middleBand - (standardDeviations * stdDev)
        val percentB = (currentPrice - lowerBand) / (upperBand - lowerBand)
        val bandWidth = (upperBand - lowerBand) / middleBand

        return analyzeBollingerValues(currentPrice, upperBand, middleBand, lowerBand, percentB, bandWidth)
    }

    private fun analyzeBollingerValues(
        currentPrice: Double,
        upperBand: Double,
        middleBand: Double,
        lowerBand: Double,
        percentB: Double,
        bandWidth: Double
    ): GeneratedSignal {

        // Analyze band squeeze (low volatility)
        val isSqueezing = bandWidth < 0.10 // Band width less than 10%

        val (result, confidence, reasoning) = when {
            // Price below lower band - oversold
            percentB < 0 -> {
                val conf = if (percentB < -0.1) SignalConfidence.HIGH else SignalConfidence.MEDIUM
                Triple(
                    SignalResult.BUY,
                    conf,
                    "Price ${String.format("%.2f", (percentB * 100))}% below lower Bollinger Band - oversold condition"
                )
            }

            // Price above upper band - overbought
            percentB > 1.0 -> {
                val conf = if (percentB > 1.1) SignalConfidence.HIGH else SignalConfidence.MEDIUM
                Triple(
                    SignalResult.SELL,
                    conf,
                    "Price ${
                        String.format(
                            "%.2f",
                            ((percentB - 1) * 100)
                        )
                    }% above upper Bollinger Band - overbought condition"
                )
            }

            // Price near lower band (0-20%) - potential buy
            percentB <= 0.2 -> Triple(
                SignalResult.BUY,
                if (isSqueezing) SignalConfidence.HIGH else SignalConfidence.MEDIUM,
                "Price near lower band (${String.format("%.0f", percentB * 100)}%B)" +
                        if (isSqueezing) " with band squeeze - potential breakout" else ""
            )

            // Price near upper band (80-100%) - potential sell
            percentB >= 0.8 -> Triple(
                SignalResult.SELL,
                if (isSqueezing) SignalConfidence.HIGH else SignalConfidence.MEDIUM,
                "Price near upper band (${String.format("%.0f", percentB * 100)}%B)" +
                        if (isSqueezing) " with band squeeze - potential breakdown" else ""
            )

            // Band squeeze in middle - wait for breakout
            isSqueezing && percentB > 0.4 && percentB < 0.6 -> Triple(
                SignalResult.WAIT,
                SignalConfidence.MEDIUM,
                "Bollinger Band squeeze detected (width: ${
                    String.format(
                        "%.2f",
                        bandWidth * 100
                    )
                }%) - wait for breakout"
            )

            // Price moving toward middle band
            percentB > 0.3 && percentB < 0.7 -> {
                val direction = if (currentPrice > middleBand) "above" else "below"
                Triple(
                    SignalResult.HOLD,
                    SignalConfidence.LOW,
                    "Price $direction middle band, ${String.format("%.0f", percentB * 100)}%B - neutral position"
                )
            }

            else -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "Bollinger Bands showing neutral signals (${String.format("%.0f", percentB * 100)}%B)"
            )
        }

        // Calculate targets and stops based on band positions
        val (targetPrice, stopLossPrice) = when (result) {
            SignalResult.BUY -> {
                // Target: middle band or upper band if strong signal
                val target = if (confidence == SignalConfidence.HIGH) upperBand else middleBand
                // Stop: below lower band
                val stop = lowerBand - (currentPrice * 0.01)
                Pair(target, stop)
            }

            SignalResult.SELL -> {
                // Target: middle band or lower band if strong signal
                val target = if (confidence == SignalConfidence.HIGH) lowerBand else middleBand
                // Stop: above upper band
                val stop = upperBand + (currentPrice * 0.01)
                Pair(target, stop)
            }

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
            probability = calculateBollingerProbability(percentB, bandWidth, result),
            riskRewardRatio = if (targetPrice != null && stopLossPrice != null) {
                abs(targetPrice - currentPrice) / abs(currentPrice - stopLossPrice)
            } else null
        )
    }

    private fun calculateBollingerProbability(percentB: Double, bandWidth: Double, result: SignalResult): Double {
        val squeezeBonus = if (bandWidth < 0.10) 0.15 else 0.0

        return when (result) {
            SignalResult.BUY -> when {
                percentB < 0 -> 0.80 + squeezeBonus
                percentB < 0.2 -> 0.70 + squeezeBonus
                else -> 0.60
            }

            SignalResult.SELL -> when {
                percentB > 1.0 -> 0.80 + squeezeBonus
                percentB > 0.8 -> 0.70 + squeezeBonus
                else -> 0.60
            }

            SignalResult.WAIT -> 0.65 + squeezeBonus
            else -> 0.50
        }
    }
}