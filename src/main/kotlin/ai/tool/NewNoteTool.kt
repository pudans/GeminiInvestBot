package ru.pudans.investrobot.ai.tool

import kotlinx.serialization.json.jsonObject
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.ai.models.Parameters
import ru.pudans.investrobot.ai.models.Property
import ru.pudans.investrobot.ai.models.Type
import ru.pudans.investrobot.database.NotesRepository

class NewNoteTool(
    private val repository: NotesRepository
) : GeminiToolExecutor {

    override val name: String = "GetNoteTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Create and insert new note about the particular instrument",
        parameters = Parameters(
            type = Type.OBJECT,
            properties = mapOf(
                "figi" to Property(
                    type = Type.STRING,
                    description = "The ticker"
                ),
                "content" to Property(
                    type = Type.STRING,
                    description = "Message note that needs to be saved"
                )
            ),
            required = listOf("figi", "content")
        )
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        println("$name request")

        val figi = args.args?.jsonObject?.get("figi")?.toString() ?: error("figi is null")
        val content = args.args?.jsonObject?.get("content")?.toString() ?: error("content is null")

        repository.insertNote(figi, content)

        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = null
        )
    }
}