@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.models

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class TechAnalysisRequest(
    val indicatorType: IndicatorType,
    val instrumentUid: String,
    val from: Long,
    val to: Long,
    val interval: IndicatorInterval,
    val typeOfPrice: TypeOfPrice,
    val length: Int?,
    val deviation: Double?,
    val smoothingFastLength: Int?,
    val smoothingSlowLength: Int?,
    val smoothingSignal: Int?
)

@Serializable
data class TechnicalIndicator(
    val timestamp: Long,
    val price: Double,
    val macd: Double,
    val middleBand: Double,
    val lowerBand: Double,
    val upperBand: Double
)

@Serializable
enum class IndicatorType {
    INDICATOR_TYPE_BB,
    INDICATOR_TYPE_EMA,
    INDICATOR_TYPE_RSI,
    INDICATOR_TYPE_MACD,
    INDICATOR_TYPE_SMA
}

enum class IndicatorInterval {
    INDICATOR_INTERVAL_ONE_MINUTE,
    INDICATOR_INTERVAL_FIVE_MINUTES,
    INDICATOR_INTERVAL_FIFTEEN_MINUTES,
    INDICATOR_INTERVAL_ONE_HOUR,
    INDICATOR_INTERVAL_ONE_DAY
}

enum class TypeOfPrice {
    TYPE_OF_PRICE_CLOSE,
    TYPE_OF_PRICE_OPEN,
    TYPE_OF_PRICE_HIGH,
    TYPE_OF_PRICE_LOW,
    TYPE_OF_PRICE_AVG
}