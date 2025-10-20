package ru.pudans.investrobot

import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.message
import com.github.kotlintelegrambot.dispatcher.telegramError
import com.github.kotlintelegrambot.entities.ChatAction
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.Message
import com.github.kotlintelegrambot.entities.ReplyMarkup
import com.github.kotlintelegrambot.extensions.filters.Filter
import com.github.kotlintelegrambot.logging.LogLevel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.ai.GeminiClient3
import ru.pudans.investrobot.ai.models.Content
import ru.pudans.investrobot.ai.models.Part
import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.secrets.SecretKey

class GeminiInvestRobotApp3 : KoinComponent {

    private val getSecret: GetSecretUseCase by inject()

    private val geminiClient: GeminiClient3 by inject()

    val instructions = """
        You are an investment buddy dedicated to helping users discover investment opportunities. 
        You are equipped with various functions to perform financial data analysis. 
        When a user inquires, you are responsible for selecting the most relevant technical indicators (e.g., RSI, MACD, Moving Averages) and analysis types. 
        Do not hesitate to make multiple function calls to gather diverse insights and provide a comprehensive recommendation. 
        All analyses must be conducted with awareness of the current date and prevailing market conditions.
        in technical analyse consider the current data: ${java.time.Clock.systemUTC().instant()}
        Mine timezone is Singapore. The market timezone is Moscow.
    """.trimIndent()

    operator fun invoke() {
        bot {
            token = getSecret.invoke(SecretKey.TELEGRAM_BOT_TOKEN)
            timeout = 30
            logLevel = LogLevel.Error

            fun checkUserWhiteListed(message: Message) {
                val whiteUserList = getSecret(SecretKey.TELEGRAM_BOT_OWNER_ID)
                    .split(",").map { it.toLong() }
                if (!whiteUserList.contains(message.from?.id)) {
                    error("User ${message.from?.id} is not in white list.")
                }
            }

            fun Bot.sendErrorMessage(error: Throwable, message: Message) {
                error.printStackTrace()
                sendMessage(
                    chatId = ChatId.fromId(id = message.chat.id),
                    text = "[ERROR]: ${error.message}",
                    disableWebPagePreview = true
                )
            }

            fun Bot.sendMessage(
                text: String,
                message: Message,
                replyMarkup: ReplyMarkup? = null
            ) {
                runCatching {
                    sendChatAction(
                        chatId = ChatId.fromId(id = message.chat.id),
                        action = ChatAction.TYPING
                    )
                    checkUserWhiteListed(message)
                    sendMessage(
                        chatId = ChatId.fromId(id = message.chat.id),
                        text = text,
                        replyMarkup = replyMarkup,
                        disableWebPagePreview = true
                    )
                }.onFailure { error ->
                    sendErrorMessage(error, message)
                }
            }

            fun Bot.sendSystemMessage(text: String, message: Message) {
                sendMessage(
                    message = message,
                    text = "[SYSTEM]: $text"
                )
            }

            dispatch {
                command(command = "start") {}
                command(command = "clear_context") {
                    geminiClient.contentCache.clear()
                    bot.sendSystemMessage(text = "Context cleared!", message)
                }

                message(filter = Filter.Text) {
                    runCatching {
                        checkUserWhiteListed(message)

                        val request = message.text.orEmpty()

                        geminiClient.generateContent(
                            temperature = 0.2,
                            systemInstruction = Content(
                                parts = listOf(Part(text = instructions)),
                                role = "user"
                            ),
                            contents = listOf(
                                Content(
                                    parts = listOf(Part(text = request)),
                                    role = "user"
                                )
                            ),
                            onAnswers = {
                                bot.sendMessage(it, message)
                            }
                        )

                    }.onFailure { error -> bot.sendErrorMessage(error, message) }
                }

                telegramError { println(error.getErrorMessage()) }
            }
        }.startPolling()
    }
}