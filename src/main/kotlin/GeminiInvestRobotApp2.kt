@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.ai.GeminiClient2
import ru.pudans.investrobot.ai.models.Content
import ru.pudans.investrobot.ai.models.GeminiModel
import ru.pudans.investrobot.ai.models.Part
import kotlin.time.ExperimentalTime

class GeminiInvestRobotApp2(
//    private val config: Config
) : KoinComponent {

    val instructions = """
        You are an investment buddy dedicated to helping users discover investment opportunities. 
        You are equipped with various functions to perform financial data analysis. 
        When a user inquires, you are responsible for selecting the most relevant technical indicators (e.g., RSI, MACD, Moving Averages) and analysis types. 
        Do not hesitate to make multiple function calls to gather diverse insights and provide a comprehensive recommendation. 
        All analyses must be conducted with awareness of the current date and prevailing market conditions.
        in technical analyse consider the current date: ${java.time.Clock.systemUTC().instant()}.
        You can request for notes for particular instrument using functional call.
        Insert your any helpful notes about instrument using functional call that can be useful in future iterations.
    """.trimIndent()

    private val geminiClient: GeminiClient2 by inject()

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

            val geminiInput =
                "Get 1 random instrument, analyse it. Share the results of analyse. Provide your result as answer to question: Should I buy/sell or keep this instrument and why"
//                "I have bought the share called PHOR. Do a tech analyse of this instrument and provide the recommendation sell/buy or keep it. "
//                "Get the list of my favourite shares. do a tech analyse of them and provide the recommendation sell/buy or keep it. "

            val answer = geminiClient.generateContent(
                model = GeminiModel.FLASH_2_5,
                temperature = 0.0,
                systemInstruction = Content(
                    parts = listOf(Part(text = instructions)),
                    role = "user"
                ),
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = geminiInput)),
                        role = "user"
                    )
                )
            ).getOrThrow().parts?.first()?.text

            println(answer)
        }
    }
}