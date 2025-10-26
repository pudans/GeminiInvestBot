@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.signal

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.*
import ru.pudans.investrobot.repository.MarketDataRepository
import kotlin.math.abs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime

/**
 * Signal generator based on Moving Average crossovers and price relationships
 * - Golden Cross: Fast MA crosses above Slow MA (bullish)
 * - Death Cross: Fast MA crosses below Slow MA (bearish)
 * - Price above/below MA indicates trend direction
 */
class MovingAverageSignalGenerator : SignalGenerator, KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()

    override val name: String = "MovingAverage"
    override val priority: Int = 2

    private val fastPeriod = 20
    private val slowPeriod = 50
    private val trendPeriod = 200

    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> =
        runCatching { analyzeMovingAverages(context) }

    private suspend fun analyzeMovingAverages(context: SignalContext): GeneratedSignal {
        // Check for sufficient data
        val hourlyCandles = context.multiTimeframeCandles[CandleInterval.INTERVAL_1_HOUR]
        if (hourlyCandles == null || hourlyCandles.size < trendPeriod + 5) {
            error("Insufficient data for Moving Average analysis (need at least ${trendPeriod + 5} hourly candles)")
        }

        // Get multiple EMA data
        val fastEMA = marketDataRepository.getTechAnalysis(
            TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_EMA,
                instrumentUid = context.instrument.uid,
                from = Clock.System.now().minus(72.hours).epochSeconds,
                to = Clock.System.now().epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                length = fastPeriod
            )
        ).getOrThrow()

        // Get slow EMA
        val slowEMA = marketDataRepository.getTechAnalysis(
            TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_EMA,
                instrumentUid = context.instrument.uid,
                from = Clock.System.now().minus(72.hours).epochSeconds,
                to = Clock.System.now().epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                length = slowPeriod
            )
        ).getOrThrow()

        // Get trend EMA
        val trendEMA = marketDataRepository.getTechAnalysis(
            TechAnalysisRequest(
                indicatorType = IndicatorType.INDICATOR_TYPE_EMA,
                instrumentUid = context.instrument.uid,
                from = Clock.System.now().minus(240.hours).epochSeconds, // Need more data for 200 EMA
                to = Clock.System.now().epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                length = trendPeriod
            )
        ).getOrThrow()

        return analyzeEMAData(fastEMA, slowEMA, trendEMA, context)
    }

//    private fun calculateManualEMAs(context: SignalContext, candles: List<ru.pudans.investrobot.models.Candle>): GeneratedSignal {
//        val prices = candles.map { it.close }
//
//        if (prices.size < trendPeriod) {
//            return GeneratedSignal(
//                name = name,
//                result = SignalResult.WAIT,
//                confidence = SignalConfidence.LOW,
//                reasoning = "Insufficient price data for Moving Average calculation",
//                timeframe = CandleInterval.INTERVAL_1_HOUR
//            )
//        }
//
//        // Calculate EMAs
//        val fastEMA = calculateEMA(prices, fastPeriod)
//        val slowEMA = calculateEMA(prices, slowPeriod)
//        val trendEMA = calculateEMA(prices, trendPeriod)
//
//        val currentPrice = context.currentPrice
//        val currentFast = fastEMA.last()
//        val currentSlow = slowEMA.last()
//        val currentTrend = trendEMA.last()
//
//        val previousFast = if (fastEMA.size >= 2) fastEMA[fastEMA.size - 2] else currentFast
//        val previousSlow = if (slowEMA.size >= 2) slowEMA[slowEMA.size - 2] else currentSlow
//
//        return analyzeEMAValues(currentPrice, currentFast, currentSlow, currentTrend, previousFast, previousSlow, context)
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

    private fun analyzeEMAData(
        fastEMAData: List<TechnicalIndicator>,
        slowEMAData: List<TechnicalIndicator>,
        trendEMAData: List<TechnicalIndicator>,
        context: SignalContext
    ): GeneratedSignal {

        val currentFast = fastEMAData.last().price
        val currentSlow = slowEMAData.last().price
        val currentTrend = trendEMAData.last().price
        val currentPrice = context.currentPrice

        val previousFast = if (fastEMAData.size >= 2) fastEMAData[fastEMAData.size - 2].price else currentFast
        val previousSlow = if (slowEMAData.size >= 2) slowEMAData[slowEMAData.size - 2].price else currentSlow

        return analyzeEMAValues(currentPrice, currentFast, currentSlow, currentTrend, previousFast, previousSlow)
    }

    private fun analyzeEMAValues(
        currentPrice: Double,
        currentFast: Double,
        currentSlow: Double,
        currentTrend: Double,
        previousFast: Double,
        previousSlow: Double
    ): GeneratedSignal {

        // Detect crossovers
        val goldenCross = previousFast <= previousSlow && currentFast > currentSlow
        val deathCross = previousFast >= previousSlow && currentFast < currentSlow

        // Calculate trend alignment
        val bullishAlignment = currentPrice > currentFast && currentFast > currentSlow && currentSlow > currentTrend
        val bearishAlignment = currentPrice < currentFast && currentFast < currentSlow && currentSlow < currentTrend

        // Calculate momentum
        val fastMomentum = (currentFast - previousFast) / previousFast
        (currentSlow - previousSlow) / previousSlow

        // Calculate distance from trend line
        val trendDistance = (currentPrice - currentTrend) / currentTrend

        val (result, confidence, reasoning) = when {
            // Strong bullish signals
            goldenCross && bullishAlignment -> Triple(
                SignalResult.BUY,
                SignalConfidence.VERY_HIGH,
                "Golden Cross confirmed with full bullish alignment (EMA${fastPeriod} > EMA${slowPeriod} > EMA${trendPeriod})"
            )

            goldenCross -> Triple(
                SignalResult.BUY,
                SignalConfidence.HIGH,
                "Golden Cross: EMA${fastPeriod} crossed above EMA${slowPeriod}"
            )

            // Strong bearish signals  
            deathCross && bearishAlignment -> Triple(
                SignalResult.SELL,
                SignalConfidence.VERY_HIGH,
                "Death Cross confirmed with full bearish alignment (EMA${fastPeriod} < EMA${slowPeriod} < EMA${trendPeriod})"
            )

            deathCross -> Triple(
                SignalResult.SELL,
                SignalConfidence.HIGH,
                "Death Cross: EMA${fastPeriod} crossed below EMA${slowPeriod}"
            )

            // Trend following signals
            bullishAlignment && fastMomentum > 0.01 -> Triple(
                SignalResult.BUY,
                SignalConfidence.MEDIUM,
                "Strong bullish alignment with accelerating fast EMA (+${String.format("%.2f", fastMomentum * 100)}%)"
            )

            bearishAlignment && fastMomentum < -0.01 -> Triple(
                SignalResult.SELL,
                SignalConfidence.MEDIUM,
                "Strong bearish alignment with accelerating fast EMA (${String.format("%.2f", fastMomentum * 100)}%)"
            )

            // Price above fast EMA in uptrend
            currentPrice > currentFast && currentFast > currentSlow && trendDistance > 0.05 -> Triple(
                SignalResult.BUY,
                SignalConfidence.MEDIUM,
                "Price above EMAs with ${String.format("%.1f", trendDistance * 100)}% above trend line"
            )

            // Price below fast EMA in downtrend
            currentPrice < currentFast && currentFast < currentSlow && trendDistance < -0.05 -> Triple(
                SignalResult.SELL,
                SignalConfidence.MEDIUM,
                "Price below EMAs with ${String.format("%.1f", abs(trendDistance) * 100)}% below trend line"
            )

            // Weak signals - price near moving averages
            abs(currentPrice - currentFast) / currentFast < 0.02 -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "Price near EMA${fastPeriod} (${
                    String.format(
                        "%.2f",
                        abs(currentPrice - currentFast) / currentFast * 100
                    )
                }% away)"
            )

            else -> Triple(
                SignalResult.HOLD,
                SignalConfidence.LOW,
                "Mixed moving average signals - no clear trend direction"
            )
        }

        // Calculate targets and stops based on EMA levels
        val (targetPrice, stopLossPrice) = when (result) {
            SignalResult.BUY -> {
                // Target: next resistance level or upper EMA
                val target = if (currentFast > currentSlow) {
                    currentPrice + (currentFast - currentSlow) * 2 // Project upward movement
                } else {
                    currentFast * 1.03 // 3% above fast EMA
                }
                // Stop: below slow EMA or 2% below entry
                val stop = minOf(currentSlow * 0.98, currentPrice * 0.98)
                Pair(target, stop)
            }

            SignalResult.SELL -> {
                // Target: next support level or lower EMA
                val target = if (currentFast < currentSlow) {
                    currentPrice - (currentSlow - currentFast) * 2 // Project downward movement
                } else {
                    currentFast * 0.97 // 3% below fast EMA
                }
                // Stop: above slow EMA or 2% above entry
                val stop = maxOf(currentSlow * 1.02, currentPrice * 1.02)
                Pair(target, stop)
            }

            else -> Pair(null, null)
        }

        val riskRewardRatio = if (targetPrice != null && stopLossPrice != null) {
            abs(targetPrice - currentPrice) / abs(currentPrice - stopLossPrice)
        } else null

        return GeneratedSignal(
            name = name,
            result = result,
            confidence = confidence,
            reasoning = reasoning,
            entryPrice = currentPrice,
            targetPrice = targetPrice,
            stopLossPrice = stopLossPrice,
            timeframe = CandleInterval.INTERVAL_1_HOUR,
            probability = calculateMAProbability(
                hasCrossover = goldenCross || deathCross,
                hasAlignment = bullishAlignment || bearishAlignment,
                momentum = fastMomentum,
                result = result
            ),
            riskRewardRatio = riskRewardRatio
        )
    }

    private fun calculateMAProbability(
        hasCrossover: Boolean,
        hasAlignment: Boolean,
        momentum: Double,
        result: SignalResult
    ): Double {
        var baseProbability = 0.50

        if (hasCrossover) baseProbability += 0.20
        if (hasAlignment) baseProbability += 0.15
        if (abs(momentum) > 0.01) baseProbability += 0.10

        return when (result) {
            SignalResult.BUY, SignalResult.SELL -> baseProbability
            else -> 0.50
        }
    }
}
