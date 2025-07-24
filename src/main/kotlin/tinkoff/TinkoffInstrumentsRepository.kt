package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pudans.investrobot.models.Instrument
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.tinkoff.piapi.contract.v1.SecurityTradingStatus

class TinkoffInstrumentsRepository(
    private val api: TinkoffInvestApi
) : InstrumentsRepository {

    override suspend fun getInstrumentByTicker(
        ticker: String,
        classCode: String
    ): Result<Instrument> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.getInstrumentByTickerSync(ticker, classCode)
                .let {
                    Instrument(
                        name = it.name,
                        figi = it.figi,
                        uid = it.uid,
                        type = it.instrumentType
                    )
                }
        }
    }

    override suspend fun getInstrumentByName(name: String): Result<List<Instrument>> =
        withContext(Dispatchers.IO) {
            runCatching {
                api.instrumentsService.findInstrumentSync(name)
                    .map {
                        Instrument(
                            name = it.name,
                            figi = it.figi,
                            uid = it.uid,
                            type = it.instrumentType
                        )
                    }
            }
        }

    override suspend fun getShares(): Result<List<Instrument>> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.allSharesSync
                .filter { it.currency == "rub" }
                .filter { it.tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING }
                .map { share ->
                    Instrument(
                        figi = share.figi,
                        name = share.name,
                        uid = share.uid,
                        type = "share"
                    )
                }
        }
    }

    override suspend fun getBonds(): Result<List<Instrument>> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.allBondsSync
                .filter { it.currency == "rub" }
                .filter { it.tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING }
                .map { bond ->
                    Instrument(
                        figi = bond.figi,
                        name = bond.name,
                        uid = bond.uid,
                        type = "bond"
                    )
                }
        }
    }

    override suspend fun getFutures(): Result<List<Instrument>> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.allFuturesSync
                .filter { it.currency == "rub" }
                .filter { it.tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING }
                .map { future ->
                    Instrument(
                        figi = future.figi,
                        name = future.name,
                        uid = future.uid,
                        type = "future"
                    )
                }
        }
    }

    override suspend fun getEtfs(): Result<List<Instrument>> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.allEtfsSync
                .filter { it.currency == "rub" }
                .filter { it.tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING }
                .map { etf ->
                    Instrument(
                        figi = etf.figi,
                        name = etf.name,
                        uid = etf.uid,
                        type = "etf"
                    )
                }
        }
    }

    override suspend fun getCurrencies(): Result<List<Instrument>> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.allCurrenciesSync
                .filter { it.currency == "rub" }
                .filter { it.tradingStatus == SecurityTradingStatus.SECURITY_TRADING_STATUS_NORMAL_TRADING }
                .map { currency ->
                    Instrument(
                        figi = currency.figi,
                        name = currency.name,
                        uid = currency.uid,
                        type = "currency"
                    )
                }
        }
    }

    override suspend fun getFavorites(): Result<List<Instrument>> = withContext(Dispatchers.IO) {
        runCatching {
            api.instrumentsService.favoritesSync
                .map { favorite ->
                    Instrument(
                        figi = favorite.figi,
                        name = favorite.name,
                        uid = favorite.uid,
                        type = favorite.instrumentType
                    )
                }
        }
    }
}