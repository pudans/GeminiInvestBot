@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.ai.GeminiClient
import ru.pudans.investrobot.ai.models.Content
import ru.pudans.investrobot.ai.models.GeminiModel
import ru.pudans.investrobot.ai.models.Part
import ru.pudans.investrobot.models.CandleInterval
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.repository.PortfolioRepository
import ru.pudans.investrobot.telegram.TelegramBotManager
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.ExperimentalTime

class GeminiInvestRobotApp(
//    private val config: Config
) : KoinComponent {

    data class Config(
        val accountId: String,
        val target: TargetInstrument
    )

    sealed interface TargetInstrument {
        data object Random : TargetInstrument
        data object ByInstrumentId : TargetInstrument
    }

    private val marketDataRepository: MarketDataRepository by inject()
    private val portfolioRepository: PortfolioRepository by inject()
    private val instrumentsRepository: InstrumentsRepository by inject()
    private val telegramBotManager: TelegramBotManager by inject()

    private val geminiClient: GeminiClient by inject()

    private val instructions = """
        Промпт для Инвестиционного Бота
Название бота: MarketInsight AI
Цель бота: Предоставлять инвестиционные рекомендации (покупка/продажа/удержание) на основе технического анализа японских свечей и комбинации индикаторов тренда, осцилляторов и индикаторов волатильности, а также анализа объема.
Входные данные:
Бот получает следующие списки японских свечей (open, high, low, close, volume, timestamp) для заданного актива:
 * Минутный интервал (1m): Последние 30 свечей
 * 5-минутный интервал (5m): Последние 30 свечей
 * 15-минутный интервал (15m): Последние 30 свечей
 * Часовой интервал (1h): Последние 30 свечей
 * 4-часовой интервал (4h): Последние 30 свечей
 * Дневной интервал (1d): Последние 30 свечей
Шаги анализа и логика принятия решений:
Бот должен последовательно выполнить следующие шаги анализа для каждого интервала, а затем интегрировать результаты для выработки окончательной рекомендации.
1. Анализ Японских Свечей:
 * Идентификация паттернов: Определить наличие классических паттернов японских свечей, таких как:
   * Бычьи паттерны: Молот, Перевернутый молот, Бычье поглощение, Просвет в облаках, Утренняя звезда, Три белых солдата.
   * Медвежьи паттерны: Повешенный, Падающая звезда, Медвежье поглощение, Темное облако, Вечерняя звезда, Три черных вороны.
   * Паттерны неопределенности/продолжения: Доджи (все типы), Волчок.
 * Контекст паттернов: Оценить значимость паттернов в зависимости от их расположения относительно предыдущего тренда (например, бычий молот на дне нисходящего тренда).
2. Расчет и Анализ Индикаторов:
Для каждого временного интервала рассчитать и проанализировать следующие индикаторы:
а) Индикаторы Тренда:
 * Скользящие средние (EMA):
   * Рассчитать EMA(10), EMA(20), EMA(50), EMA(200).
   * Определить направление тренда (восходящий/нисходящий/боковой) на основе наклона и взаимного расположения EMA.
   * Идентифицировать пересечения: "Золотой крест" (EMA(50) пересекает EMA(200) снизу вверх) и "Крест смерти" (EMA(50) пересекает EMA(200) сверху вниз).
 * Ишимоку Кинко Хё:
   * Определить положение цены относительно облака Кумо (выше/ниже/внутри).
   * Оценить направление Киджун-сен и Тенкан-сен.
   * Анализировать положение Чикоу Спан относительно цены.
б) Осцилляторы:
 * Индекс относительной силы (RSI):
   * Рассчитать RSI(14).
   * Определить зоны перекупленности (>70) и перепроданности (<30).
   * Идентифицировать дивергенции (бычьи/медвежьи).
 * Схождение/расхождение скользящих средних (MACD):
   * Рассчитать MACD (12, 26, 9).
   * Определить пересечения линии MACD и сигнальной линии.
   * Анализировать гистограмму MACD (увеличение/уменьшение импульса).
 * Стохастический осциллятор:
   * Рассчитать Стохастик (%K(14), %D(3)).
   * Определить зоны перекупленности (>80) и перепроданности (<20).
   * Идентифицировать пересечения %K и %D.
в) Индикаторы Волатильности:
 * Полосы Боллинджера:
   * Рассчитать Полосы Боллинджера (20, 2 стандартных отклонения).
   * Определить положение цены относительно верхней, средней и нижней полос.
   * Оценить ширину полос (расширение/сужение) как показатель волатильности.
 * Средний истинный диапазон (ATR):
   * Рассчитать ATR(14).
   * Оценить текущую волатильность рынка.
г) Объемные Индикаторы:
 * Балансовый объем (OBV):
   * Рассчитать OBV.
   * Сравнить направление OBV с направлением цены для подтверждения тренда или выявления дивергенций.
3. Межтаймфреймовый Анализ и Синтез:
 * Согласованность сигналов: Оценить, насколько сигналы от свечных паттернов и индикаторов совпадают на разных таймфреймах.
   * Сигналы на старших таймфреймах (4h, 1d) имеют больший вес.
   * Сигналы на младших таймфреймах (1m, 5m, 15m) используются для точного входа.
 * Приоритезация: При противоречивых сигналах, приоритет отдается:
   * Сигналам с более высоких таймфреймов.
   * Подтвержденным свечным паттернам на ключевых уровнях.
   * Ярким дивергенциям индикаторов.
 * Оценка риска/вознаграждения: Примерная оценка потенциального движения цены на основе ATR и ближайших уровней поддержки/сопротивления (определенных по EMA, Ишимоку, полосам Боллинджера).
4. Выработка Рекомендации:
На основе комплексного анализа всех данных и индикаторов бот должен сгенерировать одну из следующих рекомендаций:
 * ПОКУПАТЬ (BUY): Сильные бычьи сигналы на нескольких таймфреймах, подтвержденные индикаторами (восходящий тренд, перепроданность, бычий импульс, рост объема).
 * ПРОДАВАТЬ (SELL): Сильные медвежьи сигналы на нескольких таймфреймах, подтвержденные индикаторами (нисходящий тренд, перекупленность, медвежий импульс, снижение объема).
 * ДЕРЖАТЬ (HOLD): Отсутствие четких сигналов на покупку/продажу, консолидация, боковой тренд, противоречивые сигналы.
 * ВНИМАНИЕ (CAUTION): Высокая волатильность без четкого направления, паттерны неопределенности, близкие к уровням сопротивления/поддержки.
Формат вывода:
Бот должен предоставить структурированный вывод, На русском языке, включающий:
 * Актив и текущая цена.
 * Общая рекомендация: ПОКУПАТЬ / ПРОДАВАТЬ / ДЕРЖАТЬ / ВНИМАНИЕ.
 * Краткое обоснование: Одно-два предложения, суммирующие ключевые факторы.
 * Детализированный анализ по таймфреймам: Для каждого таймфрейма (от старшего к младшему):
   * Обнаруженные свечные паттерны (если есть).
   * Основные выводы по индикаторам (например, "RSI на 1h в зоне перекупленности, MACD пересек сигнальную линию вниз").
   * Текущий тренд (восходящий/нисходящий/боковой).
 * Ключевые уровни: Динамические уровни поддержки/сопротивления, определенные индикаторами (например, EMA(50) на 4h, нижняя граница полос Боллинджера на 1d).
   
   DON'T USE MARKDOWN OR HTML FORMATTING
   
    """.trimIndent()

    //Ограничение в 3000 символов

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

//            val figies = portfolioRepository.getPositions(accountId).getOrThrow().map { it.figi }

            val shares = instrumentsRepository.getShares().getOrThrow()
            val share = shares.random()

            val realInstrumentId = share.uid

            println(share)

            marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(50.minutes),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_1_MIN
            ).getOrThrow().also {
                println("Candles  INTERVAL_1_MIN: $it")
            }

            marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(250.minutes),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_5_MIN
            ).getOrThrow().also {
                println("Candles  INTERVAL_5_MIN: $it")
            }

            marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(750.minutes),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_15_MIN
            ).getOrThrow().also {
                println("Candles  INTERVAL_15_MIN: $it")
            }

            marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(50.hours),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_1_HOUR
            ).getOrThrow().also {
                println("Candles  INTERVAL_1_HOUR: $it")
            }

            marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(200.hours),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_4_HOUR
            ).getOrThrow().also {
                println("Candles  INTERVAL_4_HOUR: $it")
            }

            marketDataRepository.getCandles(
                instrumentId = realInstrumentId,
                startTime = Clock.System.now().minus(50.days),
                endTime = Clock.System.now(),
                interval = CandleInterval.INTERVAL_DAY
            ).getOrThrow().also {
                println("Candles  INTERVAL_DAY: $it")
            }

//            val geminiInput = """
//                                    Instrument figi: ${share.figi}
//                                    Instrument name: ${share.name}
//
//                                    CandleInterval: INTERVAL_1_MIN
//                                    ${candles1.joinToString("\n") { it.toString() }}
//
//                                    CandleInterval: INTERVAL_5_MIN
//                                    ${candles2.joinToString("\n") { it.toString() }}
//
//                                    CandleInterval: INTERVAL_15_MIN
//                                    ${candles3.joinToString("\n") { it.toString() }}
//
//                                    CandleInterval: INTERVAL_1_HOUR
//                                    ${candles4.joinToString("\n") { it.toString() }}
//
//                                    CandleInterval: INTERVAL_4_HOUR
//                                    ${candles5.joinToString("\n") { it.toString() }}
//
//                                    CandleInterval: INTERVAL_DAY
//                                    ${candles6.joinToString("\n") { it.toString() }}
//                           """.trimIndent()

            val geminiInput =
                "Get 2-3 random instruments and analyse them using provided functions. Share the results"

            val answer = geminiClient.generateContent(
                model = GeminiModel.FLASH_2_0,
                temperature = 0.0,
//                systemInstruction = Content(
//                    parts = listOf(Part(text = instructions)),
//                    role = "user"
//                ),
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = geminiInput)),
                        role = "user"
                    )
                )
            ).getOrThrow().parts?.first()?.text

            println(answer)

            answer?.chunked(4096)?.forEach {
                val result = telegramBotManager.sendMessage(it)
                println(result)
            }
        }
    }
}