@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pudans.investrobot.models.AnalyticSignal
import ru.pudans.investrobot.repository.SignalRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.tinkoff.piapi.contract.v1.SignalDirection
import ru.tinkoff.piapi.contract.v1.SignalState
import kotlin.time.ExperimentalTime

class TinkoffSignalRepository(
    private val api: TinkoffInvestApi
) : SignalRepository {

    override suspend fun getSignals(): Result<List<AnalyticSignal>> = withContext(Dispatchers.IO) {
        runCatching {
            api.signalService.getSignalsSync(
                SignalDirection.SIGNAL_DIRECTION_BUY,
                SignalState.SIGNAL_STATE_CLOSED
            ).map { signal ->
                AnalyticSignal(
                    id = signal.signalId,
                    strategyId = signal.strategyId,
                    strategyName = signal.strategyName,
                    instrumentUid = signal.instrumentUid,
                    name = signal.name,
                    info = String(signal.info.encodeToByteArray(), Charsets.UTF_8),
                    createdDate = signal.createDt.instant,
                    endDate = signal.endDt.instant,
                    closeDate = signal.closeDt.instant,
                    closePrice = signal.closePrice.price,
                    initialPrice = signal.initialPrice.price,
                    targetPrice = signal.targetPrice.price,
                    probability = signal.probability
                )
            }
        }
    }
}