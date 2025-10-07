package ru.pudans.investrobot.ai

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.pudans.investrobot.ai.models.Content
import ru.pudans.investrobot.ai.models.DataType
import ru.pudans.investrobot.ai.models.FunctionCallingConfig
import ru.pudans.investrobot.ai.models.GeminiModel
import ru.pudans.investrobot.ai.models.GenerationConfig
import ru.pudans.investrobot.ai.models.Part
import ru.pudans.investrobot.ai.models.Request
import ru.pudans.investrobot.ai.models.Response
import ru.pudans.investrobot.ai.models.Tool
import ru.pudans.investrobot.ai.models.ToolConfig
import ru.pudans.investrobot.ai.tool.GetFavouriteInstrumentsTool
import ru.pudans.investrobot.ai.tool.GetInstrumentByNameTool
import ru.pudans.investrobot.ai.tool.GetInstrumentCandlesTool
import ru.pudans.investrobot.ai.tool.GetNoteTool
import ru.pudans.investrobot.ai.tool.GetRandomInstrumentTool
import ru.pudans.investrobot.ai.tool.GetTechAnalysisTool
import ru.pudans.investrobot.ai.tool.GetUserPositionsTool
import ru.pudans.investrobot.ai.tool.NewNoteTool
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
        get<GetInstrumentByNameTool>(),
        get<GetNoteTool>(),
        get<NewNoteTool>()
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

        if (response.status != HttpStatusCode.OK) {
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