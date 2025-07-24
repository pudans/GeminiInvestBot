package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.tinkoff.piapi.contract.v1.MarketDataResponse
import ru.tinkoff.piapi.core.stream.StreamProcessor
import java.util.UUID

class TinkoffMarketDataManager(
    private val instrumentId: String,
    private val api: TinkoffInvestApi
) {

    fun subscribeInfo(): Flow<MarketDataResponse> = callbackFlow {
        val streamId = UUID.randomUUID().toString()
        val streamProcessor = StreamProcessor<MarketDataResponse> { response -> trySend(response) }
        val stream =
            api.marketDataStreamService.newStream(streamId, streamProcessor) { close(cause = it) }

        stream.subscribeInfo(listOf(instrumentId))

        awaitClose { stream.cancel() }
    }

    fun subscribeTrades(): Flow<MarketDataResponse> = callbackFlow {
        val streamId = UUID.randomUUID().toString()
        val streamProcessor = StreamProcessor<MarketDataResponse> { response -> trySend(response) }
        val stream =
            api.marketDataStreamService.newStream(streamId, streamProcessor) { close(cause = it) }

        stream.subscribeTrades(listOf(instrumentId))

        awaitClose { stream.cancel() }
    }

    fun subscribeCandles(): Flow<MarketDataResponse> = callbackFlow {
        val streamId = UUID.randomUUID().toString()
        val streamProcessor = StreamProcessor<MarketDataResponse> { response -> trySend(response) }
        val stream =
            api.marketDataStreamService.newStream(streamId, streamProcessor) { close(cause = it) }

        stream.subscribeCandles(listOf(instrumentId))

        awaitClose { stream.cancel() }
    }

    fun subscribeLastPrices(): Flow<MarketDataResponse> = callbackFlow {
        val streamId = UUID.randomUUID().toString()
        val streamProcessor = StreamProcessor<MarketDataResponse> { response -> trySend(response) }
        val stream =
            api.marketDataStreamService.newStream(streamId, streamProcessor) { close(cause = it) }

        stream.subscribeLastPrices(listOf(instrumentId))

        awaitClose { stream.cancel() }
    }

    fun subscribeOrderbook(): Flow<MarketDataResponse> = callbackFlow {
        val streamId = UUID.randomUUID().toString()
        val streamProcessor = StreamProcessor<MarketDataResponse> { response -> trySend(response) }
        val stream =
            api.marketDataStreamService.newStream(streamId, streamProcessor) { close(cause = it) }

        stream.subscribeOrderbook(listOf(instrumentId))

        awaitClose { stream.cancel() }
    }
}