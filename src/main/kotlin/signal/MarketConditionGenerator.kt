package ru.pudans.investrobot.signal

import ru.pudans.investrobot.models.Candle
import ru.pudans.investrobot.models.CandleInterval

class MarketConditionGenerator {

    operator fun invoke(candles: Map<CandleInterval, List<Candle>>): MarketCondition {
        // Simple market condition logic based on recent price action
        val hourlyCandles = candles[CandleInterval.INTERVAL_1_HOUR] ?: return MarketCondition.UNKNOWN

        if (hourlyCandles.size < 10) return MarketCondition.UNKNOWN

        val recent = hourlyCandles.takeLast(10)
        val priceChange = (recent.last().close - recent.first().open) / recent.first().open
        val avgVolume = recent.map { it.volume }.average()
        val currentVolume = recent.last().volume

        return when {
            priceChange > 0.05 -> MarketCondition.TRENDING_UP
            priceChange < -0.05 -> MarketCondition.TRENDING_DOWN
            currentVolume < avgVolume * 0.5 -> MarketCondition.LOW_VOLUME
            recent.map { it.high - it.low }.average() / recent.map { it.close }
                .average() > 0.05 -> MarketCondition.VOLATILE

            else -> MarketCondition.SIDEWAYS
        }
    }
}