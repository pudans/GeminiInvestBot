package ru.pudans.investrobot.ai.tool.ai.tool.ai.tool.ai.tool // Assuming same package

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.repository.InstrumentsRepository

class GetFavouriteInstrumentsTool(
    private val instrumentsRepository: InstrumentsRepository
) : GeminiToolExecutor {

    override val name: String = "GetFavouriteInstrumentTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Fetches the user's list of favorite financial instruments.",
        parameters = null
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        println("$name request")

        val instruments = instrumentsRepository.getFavorites().getOrThrow()
        println("$name response: $instruments")

        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = JsonObject(
                content = mapOf("result" to Json.encodeToJsonElement(instruments))
            )
        )
    }
}
