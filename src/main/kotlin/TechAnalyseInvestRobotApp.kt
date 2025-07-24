@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.models.IndicatorInterval
import ru.pudans.investrobot.models.IndicatorType
import ru.pudans.investrobot.models.TypeOfPrice
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.repository.PortfolioRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.ExperimentalTime

class TechAnalyseInvestRobotApp(
    private val accountId: String
) : KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()
    private val portfolioRepository: PortfolioRepository by inject()

    private val realInstrumentId = "0d53d29a-3794-41c6-ba72-556d46bacb46" // MDMG

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

            val techPrices = marketDataRepository.getTechAnalysis(
                indicatorType = IndicatorType.INDICATOR_TYPE_EMA,
                instrumentUid = realInstrumentId,
                from = Clock.System.now().minus(1.days).epochSeconds,
                to = Clock.System.now().epochSeconds,
                interval = IndicatorInterval.INDICATOR_INTERVAL_ONE_HOUR,
                typeOfPrice = TypeOfPrice.TYPE_OF_PRICE_CLOSE,
                length = 20,
                deviation = 6.0,
                smoothingSignal = 1,
                smoothingFastLength = 1,
                smoothingSlowLength = 1
            ).getOrThrow().let {
                it.map { it.price }
            }

            val realPrices = marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(1.days),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_1_HOUR
            ).getOrThrow().let {
                it.map { it.close }
            }

            println("techPrices: $techPrices")
            println("realPrices: $realPrices")
        }
    }
}