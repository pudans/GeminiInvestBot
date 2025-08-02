package ru.pudans.investrobot.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.pudans.investrobot.ai.models.*
import ru.pudans.investrobot.ai.tool.GetInstrumentCandlesTool
import ru.pudans.investrobot.ai.tool.GetRandomInstrumentTool
import ru.pudans.investrobot.ai.tool.GetTechAnalysisTool
import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.secrets.SecretKey

class GeminiClient(
    private val getSecret: GetSecretUseCase,
    private val httpClient: HttpClient
) : KoinComponent {

    suspend fun generateContent(
        model: GeminiModel = GeminiModel.FLASH_2_5,
        temperature: Double = 1.0,
        systemInstruction: Content? = null,
        contents: List<Content>
    ): Result<Content> {
        val apiKey = getSecret(SecretKey.GEMINI_API_KEY)
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/${model.rawName}:generateContent?key=$apiKey"
        val request = Request(
            contents = contents,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                temperature = temperature,
//                responseModalities = model.supportedOutputTypes.map { it.name }
            ),
            tools = listOf(
                get<GetRandomInstrumentTool>(),
                get<GetTechAnalysisTool>(),
                get<GetInstrumentCandlesTool>()
            ).let {
                listOf(
                    Tool(
                        functionDeclarations = it.map { it.declaration }
                    )
                )
            },
            safetySettings = listOf(
                SafetySettings(
                    category = "HARM_CATEGORY_HATE_SPEECH",
                    threshold = "BLOCK_NONE"
                ),
                SafetySettings(
                    category = "HARM_CATEGORY_SEXUALLY_EXPLICIT",
                    threshold = "BLOCK_NONE"
                ),
                SafetySettings(
                    category = "HARM_CATEGORY_DANGEROUS_CONTENT",
                    threshold = "BLOCK_NONE"
                ),
                SafetySettings(
                    category = "HARM_CATEGORY_HARASSMENT",
                    threshold = "BLOCK_NONE"
                ),
                SafetySettings(
                    category = "HARM_CATEGORY_CIVIC_INTEGRITY",
                    threshold = "BLOCK_NONE"
                )
            )
        )
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.Companion.OK) {
            return Result.failure(Error(response.bodyAsText()))
        }

        println("Got a response: ${response.bodyAsText()}")

        return response.body<Response>().candidates?.firstOrNull()?.content?.let {
            Result.success(it)
        } ?: run {
            Result.failure(Error("Response is null!"))
        }
    }
}