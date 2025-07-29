package ru.pudans.investrobot.ai.tool.ai.tool

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.repository.InstrumentsRepository

class GetRandomInstrumentTool(
    private val instrumentsRepository: InstrumentsRepository
) : GeminiToolExecutor {

    override val name: String = "GetRandomInstrumentTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Returns a random instrument from the available instruments in the instruments repository. Useful for getting a random stock for analysis or demonstration purposes."
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        println("$name request")

        val shares = instrumentsRepository.getShares().getOrThrow()
        val share = shares.random()
        println("$name response: $share")

        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = Json.encodeToJsonElement(share)
        )
    }
} 