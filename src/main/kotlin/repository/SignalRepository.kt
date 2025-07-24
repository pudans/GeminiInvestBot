package ru.pudans.investrobot.repository

import ru.pudans.investrobot.models.AnalyticSignal

interface SignalRepository {
    suspend fun getSignals(): Result<List<AnalyticSignal>>
} 