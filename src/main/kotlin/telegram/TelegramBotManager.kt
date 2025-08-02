package ru.pudans.investrobot.telegram

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.logging.LogLevel
import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.secrets.SecretKey

class TelegramBotManager(
    private val getSecret: GetSecretUseCase
) {

    val bot by lazy {
        bot {
            token = getSecret(SecretKey.TELEGRAM_BOT_TOKEN)
            timeout = 30
            logLevel = LogLevel.Error
        }
    }

    fun sendMessage(message: String): Any? {

//        val me = bot.getMe().onError {
//            println(it)
//        }.getOrNull()
//        println(me)

        val message = message
//            .replace("_", "\\_")
//            .replace("*", "\\*")
//            .replace("`", "\\`")
//            .replace("!", "\\!")
//            .replace("~", "\\~")
//            .replace("#", "\\#")
//            .replace("+", "\\+")
//            .replace("=", "\\=")
//            .replace("|", "\\|")
//            .replace(".", "\\.")
//            .replace(",", "\\,")
//            .replace("-", "\\-")
//
//            .replace("[", "\\[")
//            .replace("]", "\\]")
//
//            .replace("{", "\\{")
//            .replace("}", "\\}")
//
//            .replace("(", "\\(")
//            .replace(")", "\\)")

        return bot.sendMessage(
            chatId = ChatId.fromId(id = getSecret(SecretKey.TELEGRAM_BOT_OWNER_ID).toLong()),
            text = message,
//            parseMode = ParseMode.MARKDOWN
        ).onError {
            println("Telegram error: $it")
        }.getOrNull()
    }
}