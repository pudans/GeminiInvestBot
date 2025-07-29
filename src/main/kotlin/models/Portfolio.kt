package ru.pudans.investrobot.models

import kotlinx.serialization.Serializable

@Serializable
data class Portfolio(
    val totalAmount: Double,
    val yieldInPercent: Double,
    val positions: List<Position>
)

@Serializable
data class Position(
    val instrument: Instrument,
    val currentPrice: Double,
    val quantity: Int,
    val yieldAmount: Double,
)