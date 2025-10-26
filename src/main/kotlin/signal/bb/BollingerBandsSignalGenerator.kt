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
 * Configuration for Bollinger Bands Signal Generator
 */
data class BollingerBandsConfig(
    val period: Int = 20,
    val standardDeviations: Double = 2.0,
    val squeezeThreshold: Double = 0.10,     // Band width threshold for squeeze detection (10%)
    val oversoldThreshold: Double = 0.2,     // %B threshold for oversold (20%)
    val overboughtThreshold: Double = 0.8,   // %B threshold for overbought (80%)
    val extremeOversoldThreshold: Double = -0.1, // %B threshold for extreme oversold
    val extremeOverboughtThreshold: Double = 1.1, // %B threshold for extreme overbought
    val stopLossBufferPercent: Double = 0.01     // Additional buffer for stop loss (1%)
)

/**
 * Signal generator based on Bollinger Bands indicator
 * Bollinger Bands help identify overbought/oversold conditions and volatility
 * - Price touching upper band = potential sell signal (overbought)
 * - Price touching lower band = potential buy signal (oversold)
 * - Band squeeze = low volatility, potential breakout coming
 */
class BollingerBandsSignalGenerator(
    private val config: BollingerBandsConfig = BollingerBandsConfig()
) : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "BollingerBands"
    override val priority: Int = 2

    // Default constructor for Koin compatibility
    constructor() : this(BollingerBandsConfig())

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeBollingerBands(context) }

    private suspend fun analyzeBollingerBands(context: SignalContext): GeneratedSignal {
        // Check for sufficient data
        val hourlyCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]
        if (hourlyCandles == null || hourlyCandles.size < config.period + 5) {
            error("Insufficient data for Bollinger Bands analysis (need at least ${config.period + 5} hourly candles)")
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
                length = config.period,
                deviation = config.standardDeviations
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
        val recentCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]?.takeLast(config.period)
        if (recentCandles == null || recentCandles.size < config.period) {
            error("Cannot calculate Bollinger Bands without sufficient recent data")
        }

        val recentPrices = recentCandles.map { it.close }
        val variance = recentPrices.map { (it - middleBand).pow(2) }.average()
        val stdDev = sqrt(variance)

        val upperBand = middleBand + (config.standardDeviations * stdDev)
        val lowerBand = middleBand - (config.standardDeviations * stdDev)
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
        val isSqueezing = bandWidth < config.squeezeThreshold

        val (result, confidence, reasoning) = when {
            // Price below lower band - oversold
            percentB < 0 -> {
                val conf =
                    if (percentB < config.extremeOversoldThreshold) SignalConfidence.HIGH else SignalConfidence.MEDIUM
                Triple(
                    SignalResult.BUY,
                    conf,
                    "Price ${String.format("%.2f", (percentB * 100))}% below lower Bollinger Band - oversold condition"
                )
            }

            // Price above upper band - overbought
            percentB > 1.0 -> {
                val conf =
                    if (percentB > config.extremeOverboughtThreshold) SignalConfidence.HIGH else SignalConfidence.MEDIUM
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

            // Price near lower band - potential buy
            percentB <= config.oversoldThreshold -> Triple(
                SignalResult.BUY,
                if (isSqueezing) SignalConfidence.HIGH else SignalConfidence.MEDIUM,
                "Price near lower band (${String.format("%.0f", percentB * 100)}%B)" +
                        if (isSqueezing) " with band squeeze - potential breakout" else ""
            )

            // Price near upper band - potential sell
            percentB >= config.overboughtThreshold -> Triple(
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
                val stop = lowerBand - (currentPrice * config.stopLossBufferPercent)
                Pair(target, stop)
            }

            SignalResult.SELL -> {
                // Target: middle band or lower band if strong signal
                val target = if (confidence == SignalConfidence.HIGH) lowerBand else middleBand
                // Stop: above upper band
                val stop = upperBand + (currentPrice * config.stopLossBufferPercent)
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
        val squeezeBonus = if (bandWidth < config.squeezeThreshold) 0.15 else 0.0

        return when (result) {
            SignalResult.BUY -> when {
                percentB < 0 -> 0.80 + squeezeBonus
                percentB < config.oversoldThreshold -> 0.70 + squeezeBonus
                else -> 0.60
            }

            SignalResult.SELL -> when {
                percentB > 1.0 -> 0.80 + squeezeBonus
                percentB > config.overboughtThreshold -> 0.70 + squeezeBonus
                else -> 0.60
            }

            SignalResult.WAIT -> 0.65 + squeezeBonus
            else -> 0.50
        }
    }
}