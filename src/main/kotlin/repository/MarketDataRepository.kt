@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.repository

import ru.pudans.investrobot.models.Candle
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.models.CandlesRequest
import ru.pudans.investrobot.models.IndicatorInterval
import ru.pudans.investrobot.models.IndicatorType
import ru.pudans.investrobot.models.TechAnalysisRequest
import ru.pudans.investrobot.models.TechnicalIndicator
import ru.pudans.investrobot.models.TypeOfPrice
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

interface MarketDataRepository {

    suspend fun getCandles(
        instrumentId: String,
        startTime: Instant,
        endTime: Instant,
        interval: CandleInterval
    ): Result<List<Candle>>

    suspend fun getCandles(
        request: CandlesRequest
    ): Result<List<Candle>>

    suspend fun getTechAnalysis(
        indicatorType: IndicatorType,
        instrumentUid: String,
        from: Long,
        to: Long,
        interval: IndicatorInterval,
        typeOfPrice: TypeOfPrice,
        length: Int? = null,
        deviation: Double? = null,
        smoothingFastLength: Int? = null,
        smoothingSlowLength: Int? = null,
        smoothingSignal: Int? = null
    ): Result<List<TechnicalIndicator>>

    suspend fun getTechAnalysis(
        request: TechAnalysisRequest
    ): Result<List<TechnicalIndicator>>
} 