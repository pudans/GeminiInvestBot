package ru.pudans.investrobot.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.pudans.investrobot.ai.models.*
import ru.pudans.investrobot.ai.tool.*
import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.secrets.SecretKey

class GeminiClient3(
    private val getSecret: GetSecretUseCase,
    private val httpClient: HttpClient
) : KoinComponent {

    val executors = listOf(
        get<GetRandomInstrumentTool>(),
        get<GetTechAnalysisTool>(),
        get<GetInstrumentCandlesTool>(),
        get<GetUserPositionsTool>(),
        get<GetFavouriteInstrumentsTool>(),
        get<GetInstrumentByNameTool>()
    )

    val contentCache = mutableListOf<Content>()

    suspend fun generateContent(
        model: GeminiModel = GeminiModel.FLASH_2_5,
        temperature: Double = 1.0,
        systemInstruction: Content? = null,
        contents: List<Content>,
        onAnswers: (String) -> Unit
    ) {
        val apiKey = getSecret(SecretKey.GEMINI_API_KEY)
        val url =
            "https://generativelanguage.googleapis.com/v1beta/models/${model.rawName}:generateContent?key=$apiKey"

        contentCache.addAll(contents)

        val request = Request(
            contents = contentCache,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                temperature = temperature,
                responseModalities = listOf(DataType.TEXT).map { it.name }
            ),
            tools = listOf(
                Tool(
                    functionDeclarations = executors.map { it.declaration }
                )
            ),
            toolConfig = ToolConfig(
                functionCallingConfig = FunctionCallingConfig(
                    mode = "AUTO"
                )
            )
        )
        val response = httpClient.post(url) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.Companion.OK) {
            error(response.bodyAsText())
        }

        val candidate = response.body<Response>().candidates?.firstOrNull()
        val content = candidate?.content
        val parts = content?.parts
        val funCalls = parts?.mapNotNull { it.functionCall }

        println(response.body<Response>())

        contentCache.add(content!!)

        val textAnswers = parts?.mapNotNull { it.text }
        textAnswers?.onEach { onAnswers(it) }

        if (funCalls != null && funCalls.isNotEmpty()) {

            funCalls.onEach {
                onAnswers("Calling tool: ${it.name}...")
            }

            val responses = funCalls.map { funCall ->
                executors.first { it.name == funCall.name }.execute(funCall)
            }

            val newContents = listOf(
                Content(
                    parts = responses.map {
                        Part(
                            functionResponse = it
                        )
                    },
                    role = "user"
                )
            )

            generateContent(
                model = model,
                temperature = temperature,
                systemInstruction = systemInstruction,
                contents = newContents,
                onAnswers = onAnswers
            )
        }
    }
}