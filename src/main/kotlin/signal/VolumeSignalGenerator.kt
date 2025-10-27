@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.signal

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.Candle
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.tinkoff.currentTime
import kotlin.math.abs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Configuration for Volume Signal Generator
 */
data class VolumeConfig(
    val stopLossPercent: Double = 0.02,                // 2% stop loss
    val targetPercent: Double = 0.03,                  // 3% target
    val highVolumeThreshold: Double = 1.5,             // Volume ratio threshold for high volume
    val lowVolumeThreshold: Double = 0.7,              // Volume ratio threshold for low volume
    val veryHighVolumeThreshold: Double = 2.0,         // Volume ratio threshold for very high volume
    val suspectLowVolumeThreshold: Double = 0.5,       // Volume ratio threshold for suspiciously low volume
    val strongPriceChangeThreshold: Double = 0.02,     // 2% price change threshold
    val moderatePriceChangeThreshold: Double = 0.01,   // 1% price change threshold
    val significantPriceChangeThreshold: Double = 0.015, // 1.5% price change threshold
    val minimalPriceChangeThreshold: Double = 0.005    // 0.5% minimal price change threshold
)

/**
 * Signal generator based on volume analysis
 * Volume confirms price movements and indicates strength of trends
 * - High volume + price increase = strong bullish signal
 * - High volume + price decrease = strong bearish signal
 * - Low volume movements = weak signals, potential reversals
 */
class VolumeSignalGenerator(
    private val config: VolumeConfig = VolumeConfig()
) : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "Volume"
    override val priority: Int = 1 // Lower priority as volume is confirming indicator

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeVolumePatterns(context) }

    private suspend fun analyzeVolumePatterns(context: SignalContext): GeneratedSignal {
        // Analyze multiple timeframes for volume confirmation
        val hourlyCandles = marketDataRepository.getCandles(
            instrumentId = context.instrument.uid,
            startTime = currentTime.minus(24.hours),
            endTime = currentTime,
            interval = CandleInterval.INTERVAL_1_HOUR
        ).getOrThrow()

        val fifteenMinCandles = marketDataRepository.getCandles(
            instrumentId = context.instrument.uid,
            startTime = currentTime.minus(450.minutes),
            endTime = currentTime,
            interval = CandleInterval.INTERVAL_15_MIN
        ).getOrThrow()

        val fiveMinCandles = marketDataRepository.getCandles(
            instrumentId = context.instrument.uid,
            startTime = currentTime.minus(150.minutes),
            endTime = currentTime,
            interval = CandleInterval.INTERVAL_5_MIN
        ).getOrThrow()

        if (hourlyCandles.size < 10) {
            error("Insufficient volume data for analysis (need at least 10 hourly candles)")
        }

        // Analyze hourly volume patterns (primary)
        val hourlyAnalysis = analyzeTimeframeVolume(hourlyCandles, "1H")

        // Analyze shorter timeframes for confirmation
        val fifteenMinAnalysis = if (fifteenMinCandles.size >= 5) {
            analyzeTimeframeVolume(fifteenMinCandles.takeLast(5), "15M")
        } else null

        val fiveMinAnalysis = if (fiveMinCandles.size >= 5) {
            analyzeTimeframeVolume(fiveMinCandles.takeLast(5), "5M")
        } else null

        return combineVolumeAnalysis(hourlyAnalysis, fifteenMinAnalysis, fiveMinAnalysis, context)
    }

    private fun analyzeTimeframeVolume(
        candles: List<Candle>,
        timeframe: String
    ): VolumeAnalysis {
        if (candles.size < 3) {
            error("Insufficient data")
        }
        
        // Calculate volume metrics
        val recentVolumes = candles.map { it.volume }
        val avgVolume = recentVolumes.average()
        val currentVolume = recentVolumes.last()
        val volumeRatio = currentVolume.toDouble() / avgVolume

        // Calculate price changes with volume
        val currentCandle = candles.last()
        val previousCandle = candles[candles.size - 2]

        val priceChange = (currentCandle.close - previousCandle.close) / previousCandle.close
        val bodySize = abs(currentCandle.close - currentCandle.open) / currentCandle.open
        val candleRange = (currentCandle.high - currentCandle.low) / currentCandle.low

        // Volume trend analysis
        val volumeTrend = when {
            volumeRatio > config.highVolumeThreshold -> VolumeTrend.INCREASING
            volumeRatio < config.lowVolumeThreshold -> VolumeTrend.DECREASING
            else -> VolumeTrend.NEUTRAL
        }

        // Analyze volume-price relationship
        val (signal, confidence, reasoning) = when {
            // High volume bullish signals
            priceChange > config.strongPriceChangeThreshold && volumeRatio > config.veryHighVolumeThreshold -> Triple(
                SignalResult.BUY,
                SignalConfidence.HIGH,
                "$timeframe: Strong price increase (+${
                    String.format(
                        "%.2f",
                        priceChange * 100
                    )
                }%) with high volume (${String.format("%.1f", volumeRatio)}x avg)"
            )

            priceChange > config.moderatePriceChangeThreshold && volumeRatio > config.highVolumeThreshold -> Triple(
                SignalResult.BUY,
                SignalConfidence.MEDIUM,
                "$timeframe: Price increase (+${
                    String.format(
                        "%.2f",
                        priceChange * 100
                    )
                }%) with above-average volume (${String.format("%.1f", volumeRatio)}x avg)"
            )

            // High volume bearish signals
            priceChange < -config.strongPriceChangeThreshold && volumeRatio > config.veryHighVolumeThreshold -> Triple(
                SignalResult.SELL,
                SignalConfidence.HIGH,
                "$timeframe: Strong price decline (${
                    String.format(
                        "%.2f",
                        priceChange * 100
                    )
                }%) with high volume (${String.format("%.1f", volumeRatio)}x avg)"
            )

            priceChange < -config.moderatePriceChangeThreshold && volumeRatio > config.highVolumeThreshold -> Triple(
                SignalResult.SELL,
                SignalConfidence.MEDIUM,
                "$timeframe: Price decline (${
                    String.format(
                        "%.2f",
                        priceChange * 100
                    )
                }%) with above-average volume (${String.format("%.1f", volumeRatio)}x avg)"
            )

            // Low volume warnings (potential reversals)
            abs(priceChange) > config.significantPriceChangeThreshold && volumeRatio < config.suspectLowVolumeThreshold -> Triple(
                SignalResult.WAIT,
                SignalConfidence.MEDIUM,
                "$timeframe: Significant price move (${
                    String.format(
                        "%.2f",
                        priceChange * 100
                    )
                }%) on low volume (${String.format("%.1f", volumeRatio)}x avg) - suspect"
            )

            // Volume surge without price movement (accumulation/distribution)
            abs(priceChange) < config.minimalPriceChangeThreshold && volumeRatio > config.veryHighVolumeThreshold -> Triple(
                SignalResult.WAIT,
                SignalConfidence.MEDIUM,
                "$timeframe: High volume (${
                    String.format(
                        "%.1f",
                        volumeRatio
                    )
                }x avg) with minimal price movement - potential breakout setup"
            )

            // Normal volume, normal price action
            else -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "$timeframe: Normal volume/price relationship (${String.format("%.1f", volumeRatio)}x avg vol)"
            )
        }

        return VolumeAnalysis(
            timeframe = timeframe,
            signal = signal,
            confidence = confidence,
            reasoning = reasoning,
            volumeTrend = volumeTrend,
            volumeStrength = volumeRatio,
            priceChange = priceChange,
            bodySize = bodySize,
            candleRange = candleRange
        )
    }

    private fun combineVolumeAnalysis(
        hourly: VolumeAnalysis,
        fifteenMin: VolumeAnalysis?,
        fiveMin: VolumeAnalysis?,
        context: SignalContext
    ): GeneratedSignal {

        // Hourly analysis has the most weight
        var finalResult = hourly.signal
        var finalConfidence = hourly.confidence
        var reasoning = hourly.reasoning

        // Adjust based on shorter timeframes
        val confirmingSignals = mutableListOf<VolumeAnalysis>()
        val conflictingSignals = mutableListOf<VolumeAnalysis>()

        listOfNotNull(fifteenMin, fiveMin).forEach { analysis ->
            when {
                analysis.signal == hourly.signal -> confirmingSignals.add(analysis)
                analysis.signal == SignalResult.HOLD || analysis.signal == SignalResult.WAIT -> {
                    // Neutral, doesn't affect much
                }

                else -> conflictingSignals.add(analysis)
            }
        }

        // Adjust confidence based on confirmation
        when {
            confirmingSignals.size >= 2 -> {
                finalConfidence = when (finalConfidence) {
                    SignalConfidence.LOW -> SignalConfidence.MEDIUM
                    SignalConfidence.MEDIUM -> SignalConfidence.HIGH
                    SignalConfidence.HIGH -> SignalConfidence.VERY_HIGH
                    else -> finalConfidence
                }
                reasoning += ". Confirmed by shorter timeframes"
            }

            confirmingSignals.size == 1 -> {
                reasoning += ". Partially confirmed by ${confirmingSignals.first().timeframe}"
            }

            conflictingSignals.isNotEmpty() -> {
                finalConfidence = SignalConfidence.LOW
                finalResult = SignalResult.HOLD
                reasoning =
                    "Mixed volume signals across timeframes: $reasoning. Conflicts: ${conflictingSignals.joinToString(", ") { "${it.timeframe}: ${it.signal}" }}"
            }
        }

        // Add volume trend information
        val volumeTrendDescription = when (hourly.volumeTrend) {
            VolumeTrend.INCREASING -> "Volume trend: Increasing"
            VolumeTrend.DECREASING -> "Volume trend: Decreasing"
            VolumeTrend.NEUTRAL -> "Volume trend: Neutral"
        }
        reasoning += ". $volumeTrendDescription"

        val currentPrice = context.currentPrice
        val stopLossDistance = currentPrice * config.stopLossPercent
        val targetDistance = currentPrice * config.targetPercent

        val (targetPrice, stopLossPrice) = when (finalResult) {
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
            result = finalResult,
            confidence = finalConfidence,
            reasoning = reasoning,
            entryPrice = currentPrice,
            targetPrice = targetPrice,
            stopLossPrice = stopLossPrice,
            timeframe = CandleInterval.INTERVAL_1_HOUR,
            probability = calculateVolumeProbability(hourly.volumeStrength, confirmingSignals.size, finalResult)
        )
    }

    private fun calculateVolumeProbability(
        volumeStrength: Double,
        confirmingSignals: Int,
        result: SignalResult
    ): Double {
        var probability = 0.50

        // Volume strength factor
        when {
            volumeStrength > 3.0 -> probability += 0.25
            volumeStrength > config.veryHighVolumeThreshold -> probability += 0.20
            volumeStrength > config.highVolumeThreshold -> probability += 0.15
            volumeStrength < config.suspectLowVolumeThreshold -> probability -= 0.15
        }

        // Confirmation factor
        probability += confirmingSignals * 0.10

        return probability.coerceIn(0.20, 0.90)
    }

    private data class VolumeAnalysis(
        val timeframe: String,
        val signal: SignalResult,
        val confidence: SignalConfidence,
        val reasoning: String,
        val volumeTrend: VolumeTrend,
        val volumeStrength: Double,
        val priceChange: Double = 0.0,
        val bodySize: Double = 0.0,
        val candleRange: Double = 0.0
    )

    private enum class VolumeTrend {
        INCREASING,
        DECREASING,
        NEUTRAL
    }
}
