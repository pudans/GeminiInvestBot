@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.ai.ai.GeminiClient2
import ru.pudans.investrobot.ai.models.Content
import ru.pudans.investrobot.ai.models.GeminiModel
import ru.pudans.investrobot.ai.models.Part
import kotlin.time.ExperimentalTime

class GeminiInvestRobotApp2(
//    private val config: Config
) : KoinComponent {

    private val geminiClient: GeminiClient2 by inject()

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

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
        }
    }
}