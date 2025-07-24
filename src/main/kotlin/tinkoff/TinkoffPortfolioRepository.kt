package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pudans.investrobot.models.Instrument
import ru.pudans.investrobot.models.Portfolio
import ru.pudans.investrobot.models.Position
import ru.pudans.investrobot.repository.PortfolioRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi

class TinkoffPortfolioRepository(
    private val api: TinkoffInvestApi
) : PortfolioRepository {

    override suspend fun getPositions(accountId: String): Result<List<Position>> =
        withContext(Dispatchers.IO) {
            runCatching {
                getPortfolio(accountId).getOrThrow().positions
            }
        }

    override suspend fun getPortfolio(accountId: String): Result<Portfolio> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.operationsService.getPortfolioSync(accountId).let { portfolio ->
                    Portfolio(
                        yieldInPercent = portfolio.expectedYield.toDouble(),
                        totalAmount = portfolio.totalAmountPortfolio.value.toDouble(),
                        positions = portfolio.positions.map { position ->
                            Position(
                                currentPrice = position.currentPrice.value.toDouble(),
                                quantity = position.quantity.toInt(),
                                yieldAmount = position.expectedYield.toDouble(),
                                instrument = Instrument(
                                    figi = position.figi,
                                    uid = position.instrumentUid,
                                    type = position.instrumentType,
                                    name = ""
                                )
                            )
                        }
                    )
                }
            }
        }
}