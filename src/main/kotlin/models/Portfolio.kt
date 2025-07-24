package ru.pudans.investrobot.models

data class Portfolio(
    val totalAmount: Double,
    val yieldInPercent: Double,
    val positions: List<Position>
)

data class Position(
    val instrument: Instrument,
    val currentPrice: Double,
    val quantity: Int,
    val yieldAmount: Double,
)