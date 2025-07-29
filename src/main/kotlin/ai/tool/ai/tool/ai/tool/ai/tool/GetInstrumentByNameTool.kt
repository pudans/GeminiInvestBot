package ru.pudans.investrobot.ai.tool.ai.tool.ai.tool.ai.tool // Assuming same package for now

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.ai.models.Parameters
import ru.pudans.investrobot.ai.models.Property
import ru.pudans.investrobot.ai.models.Type
import ru.pudans.investrobot.repository.InstrumentsRepository

class GetInstrumentByNameTool(
    private val instrumentsRepository: InstrumentsRepository
) : GeminiToolExecutor {

    override val name: String = "GetInstrumentByNameTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Finds financial instruments by their name or ticker.",
        parameters = Parameters(
            type = Type.OBJECT,
            properties = mapOf(
                "query" to Property(
                    type = Type.STRING,
                    description = "The name, ticker, or part of the name of the instrument to search for."
                )
            ),
            required = listOf("query")
        )
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        val query = args.args!!.jsonObject["query"]?.toString()
            ?.toCharArray()
            ?.filter { it.isLetterOrDigit() }
            ?.joinToString(separator = "")
            ?: error("Missing or invalid 'query' argument")
        println("$name request: $query")

        val response = instrumentsRepository.getInstrumentByName(query).getOrThrow()
        println("$name response: $response")

        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = JsonObject(
                content = mapOf("result" to Json.encodeToJsonElement(response))
            )
        )
    }
}