@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.models

import kotlinx.serialization.Serializable
import kotlin.time.ExperimentalTime

@Serializable
data class Candle(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long,
    val time: Long
)

@Serializable
data class CandlesRequest(
    val instrumentId: String,
    val startTime: Long,
    val endTime: Long,
    val interval: CandleInterval
)