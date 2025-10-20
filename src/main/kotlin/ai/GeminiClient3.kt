package ru.pudans.investrobot.ai

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.koin.core.component.KoinComponent
import ru.pudans.investrobot.ai.models.*
import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.secrets.SecretKey

class GeminiClient3(
    private val getSecret: GetSecretUseCase,
    private val httpClient: HttpClient,
    private val toolManager: GeminiToolManager
) : KoinComponent {

    private val model: GeminiModel = GeminiModel.FLASH_2_5
    val contentCache = mutableListOf<Content>()

    private fun getUrl(): String {
        val apiKey = getSecret(SecretKey.GEMINI_API_KEY)
        return "https://generativelanguage.googleapis.com/v1beta/models/${model.rawName}:generateContent?key=$apiKey"
    }

    suspend fun generateContent(
        temperature: Double = 1.0,
        systemInstruction: Content? = null,
        contents: List<Content>,
        onAnswers: (String) -> Unit
    ) {
        contentCache.addAll(contents)

        val request = Request(
            contents = contentCache,
            systemInstruction = systemInstruction,
            generationConfig = GenerationConfig(
                temperature = temperature,
                responseModalities = listOf(DataType.TEXT.name)
            ),
            tools = listOf(
                Tool(functionDeclarations = toolManager.getDeclarations())
            ),
            toolConfig = ToolConfig(
                functionCallingConfig = FunctionCallingConfig(
                    mode = "AUTO"
                )
            )
        )
        val response = httpClient.post(urlString = getUrl()) {
            contentType(type = ContentType.Application.Json)
            setBody(request)
        }

        if (response.status != HttpStatusCode.OK) {
            error(response.bodyAsText())
        }

        val candidate = response.body<Response>().candidates?.first()
        val content = requireNotNull(candidate?.content)
        val parts = requireNotNull(content.parts)
        val functionCalls = parts.mapNotNull { it.functionCall }

        contentCache.add(content)

        val textAnswers = parts.mapNotNull { it.text }
        textAnswers.onEach { onAnswers(it) }

        if (functionCalls.isNotEmpty()) {

            val responses = functionCalls.map { funCall ->
                onAnswers("Calling tool: ${funCall.name}...")
                toolManager.getTool(funCall.name)?.execute(funCall)
            }

            val newContents = listOf(
                Content(
                    parts = responses.map { Part(functionResponse = it) },
                    role = "user"
                )
            )

            generateContent(
                temperature = temperature,
                systemInstruction = systemInstruction,
                contents = newContents,
                onAnswers = onAnswers
            )
        }
    }
}