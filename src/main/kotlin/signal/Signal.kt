package ru.pudans.investrobot.signal

import ru.pudans.investrobot.models.Candle
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.models.Instrument

enum class SignalResult {
    BUY,
    SELL,
    HOLD,
    WAIT // For when there's insufficient data or high uncertainty
}

enum class SignalConfidence {
    LOW,
    MEDIUM,
    HIGH,
    VERY_HIGH
}

data class SignalContext(
    val instrument: Instrument,
    val multiTimeframeCandles: Map<CandleInterval, List<Candle>>,
    val currentPrice: Double,
    val volume: Long,
    val marketCondition: MarketCondition? = null
)

data class GeneratedSignal(
    val name: String,
    val result: SignalResult,
    val confidence: SignalConfidence,
    val reasoning: String,
    val entryPrice: Double? = null,
    val targetPrice: Double? = null,
    val stopLossPrice: Double? = null,
    val timeframe: CandleInterval,
    val probability: Double? = null, // 0.0 to 1.0
    val riskRewardRatio: Double? = null
)

enum class MarketCondition {
    TRENDING_UP,
    TRENDING_DOWN,
    SIDEWAYS,
    VOLATILE,
    LOW_VOLUME,
    UNKNOWN
}

interface SignalGenerator {
    /**
     * Name/identifier for this signal generator
     */
    val name: String get() = this.javaClass.simpleName

    /**
     * Priority/weight for this signal generator (higher = more important)
     */
    val priority: Int get() = 1

    /**
     * Generates trading signal based on comprehensive market context
     */
    suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal>
}