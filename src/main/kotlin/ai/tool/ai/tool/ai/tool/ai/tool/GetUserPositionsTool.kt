package ru.pudans.investrobot.ai.tool.ai.tool.ai.tool.ai.tool

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import ru.pudans.investrobot.ai.GeminiToolExecutor
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse
import ru.pudans.investrobot.repository.AccountRepository
import ru.pudans.investrobot.repository.PortfolioRepository

class GetUserPositionsTool(
    private val portfolioRepository: PortfolioRepository,
    private val accountRepository: AccountRepository
) : GeminiToolExecutor {

    override val name: String = "GetUserPositionsTool"

    override val declaration: Declaration = Declaration(
        name = name,
        description = "Fetches the user's current investment positions.",
        parameters = null
    )

    override suspend fun execute(args: FunctionCall): FunctionResponse {
        val account = accountRepository.getAccounts().getOrThrow().first()
        val positions = portfolioRepository.getPositions(account.id).getOrThrow()
        return FunctionResponse(
            id = args.id,
            name = args.name,
            response = Json.encodeToJsonElement(positions).jsonObject
        )
    }
}
