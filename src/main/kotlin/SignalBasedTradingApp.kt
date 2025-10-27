@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.models.Instrument
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.signal.*
import ru.pudans.investrobot.signal.bb.BollingerBandsSignalGenerator
import ru.pudans.investrobot.signal.macd.MACDSignalGenerator
import ru.pudans.investrobot.signal.rsi.RSISignalGenerator
import ru.pudans.investrobot.telegram.TelegramBotManager
import ru.pudans.investrobot.tinkoff.currentTime
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

/**
 * Example application showing how to use the new signal architecture
 */
class SignalBasedTradingApp : KoinComponent {

    private val marketDataRepository: MarketDataRepository by inject()
    private val instrumentsRepository: InstrumentsRepository by inject()
    private val telegramBotManager: TelegramBotManager by inject()

    private val signals = listOf<SignalGenerator>(
        RSISignalGenerator(),
        MACDSignalGenerator(),
        BollingerBandsSignalGenerator(),
        MovingAverageSignalGenerator(),
        VolumeSignalGenerator()
    )

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

            val shares = instrumentsRepository.getFavorites().getOrThrow()
            val targetShare = shares.random()
            val signalContext = buildSignalContext(targetShare)

            val initMessage = String()
                .plus("📈 TRADING SIGNAL GENERATED").plus("\n")
                .plus("Instrument: ${targetShare.name} (${targetShare.figi})").plus("\n")
                .plus("Market condition: ${signalContext.marketCondition}").plus("\n")
                .plus("Volume: ${signalContext.volume}").plus("\n")
                .plus("Current price: ${signalContext.currentPrice}").plus("\n")

            sendToTelegram(initMessage)

            signals.forEach { signal ->

                val result = signal.generateSignal(signalContext).fold(
                    onSuccess = { result ->
                        String()
                            .plus("   Name: ${signal.name}").plus("\n")
                            .plus(handleTradingSignal(result))
                    },
                    onFailure = { error ->
                        String()
                            .plus("Name: ${signal.name}").plus("\n")
                            .plus("Error: ${error.message}")
                    }
                )

                sendToTelegram(result)
            }
        }
    }

    private suspend fun buildSignalContext(instrument: Instrument): SignalContext {
        // Get current market data

        val lastCandle = marketDataRepository.getCandles(
            instrumentId = instrument.uid,
            startTime = currentTime.minus(1.minutes),
            endTime = currentTime,
            interval = CandleInterval.INTERVAL_1_MIN
        ).getOrThrow()

        val latestCandle = lastCandle.lastOrNull()
        val currentPrice = latestCandle?.close ?: 0.0
        val currentVolume = latestCandle?.volume ?: 0L

        return SignalContext(
            instrument = instrument,
            currentPrice = currentPrice,
            volume = currentVolume,
//            marketCondition = MarketConditionGenerator().invoke(multiTimeframeCandles)
        )
    }

    private fun handleTradingSignal(signal: GeneratedSignal): String {
        val emoji = when (signal.result) {
            SignalResult.BUY -> "🟢 BUY"
            SignalResult.SELL -> "🔴 SELL"
            SignalResult.HOLD -> "🟡 HOLD"
            SignalResult.WAIT -> "⚪ WAIT"
        }

        val confidenceEmoji = when (signal.confidence) {
            SignalConfidence.VERY_HIGH -> "🔥🔥🔥"
            SignalConfidence.HIGH -> "🔥🔥"
            SignalConfidence.MEDIUM -> "🔥"
            SignalConfidence.LOW -> "⚡"
        }

        return String()
            .plus("$emoji Signal: ${signal.result}").plus("\n")
            .plus("$confidenceEmoji Confidence: ${signal.confidence}").plus("\n")
            .plus("💰 Entry Price: ${signal.entryPrice}").plus("\n")
            .plus("📊 Probability: ${signal.probability?.let { "${(it * 100).toInt()}%" } ?: "N/A"}").plus("\n")
            .plus("🎯 Target Price: ${signal.targetPrice ?: "N/A"}").plus("\n")
            .plus("🛡️ Stop Loss: ${signal.stopLossPrice ?: "N/A"}").plus("\n")
            .plus("📈 Risk/Reward: ${signal.riskRewardRatio?.let { "%.2f".format(it) } ?: "N/A"}").plus("\n")
            .plus("⏰ Timeframe: ${signal.timeframe}").plus("\n")
            .plus("🧠 Analysis: ${signal.reasoning}")
    }

    private fun sendToTelegram(
        message: String
    ) {
        try {
            telegramBotManager.sendMessage(message)
//            println("✅ Signal sent to Telegram")
        } catch (e: Exception) {
            println("❌ Failed to send Telegram message: ${e.message}")
        }
    }
}
