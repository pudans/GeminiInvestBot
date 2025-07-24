@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.models

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class AnalyticSignal(
    val id: String,
    val instrumentUid: String,
    val info: String,
    val strategyId: String,
    val strategyName: String,
    val name: String,
    val createdDate: Instant,
    val endDate: Instant,
    val closeDate: Instant,
    val closePrice: Double,
    val initialPrice: Double,
    val targetPrice: Double,
    val probability: Int
)