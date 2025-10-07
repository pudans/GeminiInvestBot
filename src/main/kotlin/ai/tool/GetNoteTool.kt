package ru.pudans.investrobot.ai.tool

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.ai.models.Parameters
import ru.pudans.investrobot.ai.models.Property
import ru.pudans.investrobot.ai.models.Type
import ru.pudans.investrobot.database.NotesRepository

class GetNoteTool(
    private val repository: NotesRepository
) : GeminiToolExecutor {

    override val name: String = "GetNoteTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Find notes for the particular instrument",
        parameters = Parameters(
            type = Type.OBJECT,
            properties = mapOf(
                "figi" to Property(
                    type = Type.STRING,
                    description = "The ticker"
                )
            ),
            required = listOf("figi")
        )
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        println("$name request")

        val figi = args.args?.jsonObject?.get("figi")?.toString() ?: error("figi is null")

        val notes = repository.getNotes(figi)
        println("$name response: $notes")

        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = JsonArray(
                content = notes.map { note ->
                    JsonObject(
                        content = mapOf(
                            "figi" to JsonPrimitive(note.figi),
                            "date" to JsonPrimitive(note.date.toString()),
                            "content" to JsonPrimitive(note.content)
                        )
                    )
                }
            )
        )
    }
}