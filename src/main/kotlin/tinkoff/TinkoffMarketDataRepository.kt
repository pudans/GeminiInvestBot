@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pudans.investrobot.models.Candle
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.models.CandlesRequest
import ru.pudans.investrobot.models.IndicatorInterval
import ru.pudans.investrobot.models.IndicatorType
import ru.pudans.investrobot.models.TechAnalysisRequest
import ru.pudans.investrobot.models.TechnicalIndicator
import ru.pudans.investrobot.models.TypeOfPrice
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.tinkoff.piapi.contract.v1.GetTechAnalysisRequest
import kotlin.math.floor
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlin.time.toJavaInstant
import ru.tinkoff.piapi.contract.v1.CandleInterval as TinkoffCandleInterval
import ru.tinkoff.piapi.contract.v1.GetTechAnalysisRequest.IndicatorType as TinkoffIndicatorType
import ru.tinkoff.piapi.contract.v1.Quotation as TinkoffQuotation

class TinkoffMarketDataRepository(
    private val api: TinkoffInvestApi
) : MarketDataRepository {

    override suspend fun getCandles(
        instrumentId: String,
        startTime: Instant,
        endTime: Instant,
        interval: CandleInterval
    ): Result<List<Candle>> = withContext(Dispatchers.IO) {
        runCatching {
            api.marketDataService.getCandlesSync(
                instrumentId,
                startTime.toJavaInstant(),
                endTime.toJavaInstant(),
                interval.toTinkoffInterval()
            ).map { candle ->
                Candle(
                    open = candle.open.price,
                    high = candle.high.price,
                    low = candle.low.price,
                    close = candle.close.price,
                    volume = candle.volume,
                    time = candle.time.seconds
                )
            }
        }
    }

    override suspend fun getCandles(request: CandlesRequest): Result<List<Candle>> =
        getCandles(
            instrumentId = request.instrumentId,
            startTime = request.startTime.instant,
            endTime = request.endTime.instant,
            interval = request.interval
        )

    override suspend fun getTechAnalysis(
        indicatorType: IndicatorType,
        instrumentUid: String,
        from: Long,
        to: Long,
        interval: IndicatorInterval,
        typeOfPrice: TypeOfPrice,
        length: Int?,
        deviation: Double?,
        smoothingFastLength: Int?,
        smoothingSlowLength: Int?,
        smoothingSignal: Int?
    ): Result<List<TechnicalIndicator>> = withContext(Dispatchers.IO) {
        runCatching {
            api.marketDataService.getTechAnalysisSync(
                indicatorType.toTinkoffIndicatorType(),
                instrumentUid,
                from.instant.toJavaInstant(),
                to.instant.toJavaInstant(),
                interval.toTinkoffIndicatorInterval(),
                typeOfPrice.toTinkoffTypeOfPrice(),
                length,
                deviation?.toTinkoffQuotation(),
                smoothingFastLength,
                smoothingSlowLength,
                smoothingSignal,
            ).technicalIndicatorsList.map { indicator ->
                TechnicalIndicator(
                    timestamp = indicator.timestamp.seconds,
                    price = indicator.signal.price,
                    macd = indicator.macd.price,
                    middleBand = indicator.middleBand.price,
                    lowerBand = indicator.lowerBand.price,
                    upperBand = indicator.upperBand.price
                )
            }
        }
    }

    override suspend fun getTechAnalysis(request: TechAnalysisRequest): Result<List<TechnicalIndicator>> =
        getTechAnalysis(
            indicatorType = request.indicatorType,
            instrumentUid = request.instrumentUid,
            from = request.from,
            to = request.to,
            interval = request.interval,
            typeOfPrice = request.typeOfPrice,
            length = request.length,
            deviation = request.deviation,
            smoothingSignal = request.smoothingSignal,
            smoothingFastLength = request.smoothingFastLength,
            smoothingSlowLength = request.smoothingSlowLength
        )

    private fun CandleInterval.toTinkoffInterval(): TinkoffCandleInterval =
        when (this) {
            CandleInterval.INTERVAL_1_MIN -> TinkoffCandleInterval.CANDLE_INTERVAL_1_MIN
            CandleInterval.INTERVAL_5_MIN -> TinkoffCandleInterval.CANDLE_INTERVAL_5_MIN
            CandleInterval.INTERVAL_15_MIN -> TinkoffCandleInterval.CANDLE_INTERVAL_15_MIN
            CandleInterval.INTERVAL_1_HOUR -> TinkoffCandleInterval.CANDLE_INTERVAL_HOUR
            CandleInterval.INTERVAL_4_HOUR -> TinkoffCandleInterval.CANDLE_INTERVAL_4_HOUR
            CandleInterval.INTERVAL_DAY -> TinkoffCandleInterval.CANDLE_INTERVAL_DAY
            CandleInterval.INTERVAL_WEEK -> TinkoffCandleInterval.CANDLE_INTERVAL_WEEK
            CandleInterval.INTERVAL_MONTH -> TinkoffCandleInterval.CANDLE_INTERVAL_MONTH
        }

    private fun IndicatorType.toTinkoffIndicatorType(): TinkoffIndicatorType =
        when (this) {
            IndicatorType.INDICATOR_TYPE_BB -> TinkoffIndicatorType.INDICATOR_TYPE_BB
            IndicatorType.INDICATOR_TYPE_EMA -> TinkoffIndicatorType.INDICATOR_TYPE_EMA
            IndicatorType.INDICATOR_TYPE_RSI -> TinkoffIndicatorType.INDICATOR_TYPE_RSI
            IndicatorType.INDICATOR_TYPE_MACD -> TinkoffIndicatorType.INDICATOR_TYPE_MACD
            IndicatorType.INDICATOR_TYPE_SMA -> TinkoffIndicatorType.INDICATOR_TYPE_SMA
        }

    private fun IndicatorInterval.toTinkoffIndicatorInterval(): GetTechAnalysisRequest.IndicatorInterval =
        when (this) {
            IndicatorInterval.INDICATOR_INTERVAL_ONE_MINUTE -> GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_ONE_MINUTE
            IndicatorInterval.INDICATOR_INTERVAL_FIVE_MINUTES -> GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIVE_MINUTES
            IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES -> GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_FIFTEEN_MINUTES
            IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR -> GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR
            IndicatorInterval.INDICATOR_INTERVAL_ONE_DAY -> GetTechAnalysisRequest.IndicatorInterval.INDICATOR_INTERVAL_ONE_DAY
        }

    private fun TypeOfPrice.toTinkoffTypeOfPrice(): GetTechAnalysisRequest.TypeOfPrice =
        when (this) {
            TypeOfPrice.TYPE_OF_PRICE_CLOSE -> GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_CLOSE
            TypeOfPrice.TYPE_OF_PRICE_OPEN -> GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_OPEN
            TypeOfPrice.TYPE_OF_PRICE_HIGH -> GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_HIGH
            TypeOfPrice.TYPE_OF_PRICE_LOW -> GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_LOW
            TypeOfPrice.TYPE_OF_PRICE_AVG -> GetTechAnalysisRequest.TypeOfPrice.TYPE_OF_PRICE_AVG
        }

    private fun Double.toTinkoffQuotation(): TinkoffQuotation =
        TinkoffQuotation.newBuilder()
            .setUnits(floor(this).toLong())
            .setNano(((this - floor(this)) * 1_000_000_000).toInt())
            .build()
} 