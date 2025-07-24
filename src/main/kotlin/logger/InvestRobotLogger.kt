package ru.pudans.investrobot.logger

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.pudans.investrobot.telegram.TelegramBotManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class InvestRobotLogger(
    private val telegramBotManager: TelegramBotManager
) : Logger {

    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun info(message: String) {
        val logMessage = formatLogMessage("INFO", message)
        println(logMessage)
    }

    override fun warn(message: String) {
        val logMessage = formatLogMessage("WARN", message)
        println(logMessage)
    }

    override fun error(message: String, throwable: Throwable?) {
        val fullMessage = if (throwable != null) {
            "$message\nException: ${throwable.javaClass.simpleName}: ${throwable.message}\nStackTrace: ${throwable.stackTraceToString()}"
        } else {
            message
        }

        val logMessage = formatLogMessage("ERROR", fullMessage)
        println(logMessage)

        // Send error to Telegram asynchronously
        coroutineScope.launch {
            try {
                val telegramMessage = buildTelegramErrorMessage(message, throwable)
                telegramBotManager.sendMessage(telegramMessage)
            } catch (e: Exception) {
                println("Failed to send error to Telegram: ${e.message}")
            }
        }
    }

    override fun debug(message: String) {
        val logMessage = formatLogMessage("DEBUG", message)
        println(logMessage)
    }

    private fun formatLogMessage(level: String, message: String): String {
        val timestamp = LocalDateTime.now().format(dateFormatter)
        return "[$timestamp] [$level] $message"
    }

    private fun buildTelegramErrorMessage(message: String, throwable: Throwable?): String {
        val timestamp = LocalDateTime.now().format(dateFormatter)

        return buildString {
            append("🚨 InvestRobot Error Report 🚨\n")
            append("Time: $timestamp\n")
            append("Message: $message\n")

            if (throwable != null) {
                append("Exception: ${throwable.javaClass.simpleName}\n")
                append("Details: ${throwable.message ?: "No details available"}\n")

                // Include first few lines of stack trace for context
                val stackTrace = throwable.stackTraceToString()
                val relevantStackTrace = stackTrace.lines()
                    .take(5) // Take first 5 lines to avoid overly long messages
                    .joinToString("\n")
                append("Stack Trace (top 5 lines):\n$relevantStackTrace")
            }
        }
    }
} 