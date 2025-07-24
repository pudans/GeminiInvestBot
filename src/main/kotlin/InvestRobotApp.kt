package ru.pudans.investrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.StopOrderDirection
import ru.pudans.investrobot.models.StopOrderExpirationType
import ru.pudans.investrobot.models.StopOrderType
import ru.pudans.investrobot.models.TakeProfitType
import ru.pudans.investrobot.repository.AccountRepository
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.repository.PortfolioRepository
import ru.pudans.investrobot.repository.PostOrderRepository
import ru.pudans.investrobot.repository.SignalRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.pudans.investrobot.tinkoff.price

class InvestRobotApp(
    private val accountId: String,
    private val instrumentId: String
) : KoinComponent {

    private val api: TinkoffInvestApi by inject()

    private val accountRepository: AccountRepository by inject()
    private val portfolioRepository: PortfolioRepository by inject()
    private val signalsRepository: SignalRepository by inject()
    private val instrumentsRepository: InstrumentsRepository by inject()
    private val postOrderRepository: PostOrderRepository by inject()

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

            val accountId = accountRepository.getAccounts().getOrThrow().first().id

            val instrumentId =
                instrumentsRepository.getInstrumentByTicker("ELMT", "SPBRU").getOrThrow().uid

            val price = api.marketDataService.getLastPricesSync(listOf(instrumentId)).let {
                it.maxByOrNull { it.time.seconds }!!.price.price
            }

            println(instrumentId)
            println(price)

            postOrderRepository.postOrder(
                instrumentId = instrumentId,
                quantity = 1L,
                price = price, // current market price
                stopPrice = price * 0.5, // initial stop price
                direction = StopOrderDirection.SELL,
                accountId = accountId,
                stopOrderType = StopOrderType.STOP_LOSS,
                expirationType = StopOrderExpirationType.GOOD_TILL_CANCEL,
                takeProfitType = TakeProfitType.REGULAR,
                trailingData = null
            ).onFailure {
                println(it)
            }

            // take profit
            postOrderRepository.postOrder(
                instrumentId = instrumentId,
                quantity = 1L,
                price = price, // current market price
                stopPrice = price * 1.5, // initial stop price
                direction = StopOrderDirection.SELL,
                accountId = accountId,
                stopOrderType = StopOrderType.TAKE_PROFIT,
                expirationType = StopOrderExpirationType.GOOD_TILL_CANCEL,
                takeProfitType = TakeProfitType.REGULAR,
                trailingData = null
            ).onFailure {
                println(it)
            }
        }
    }
}