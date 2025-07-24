package ru.pudans.investrobot

import java.util.Properties

enum class SecretKey {
    GEMINI_API_KEY,
    OPENAI_API_KEY,
    TELEGRAM_BOT_TOKEN,
    TELEGRAM_BOT_OWNER_ID,
    TINKOFF_ACCOUNT_PRIVATE,
    TINKOFF_ACCOUNT_BOT_EXPERIMENT,
    TINKOFF_ACCOUNT_SANDBOX
}

class GetSecretUseCase() {

    private val properties: Properties = Properties()

    init {
        val inputStream = javaClass.classLoader.getResourceAsStream("secret.properties")
        if (inputStream != null) {
            properties.load(inputStream)
        } else {
            error("secret.properties not found")
        }
    }

    operator fun invoke(key: SecretKey): String =
        requireNotNull(properties.getProperty(key.name))
}
