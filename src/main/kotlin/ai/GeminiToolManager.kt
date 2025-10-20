package ru.pudans.investrobot.ai

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.tool.*

class GeminiToolManager : KoinComponent {

    private val executors = listOf(
        get<GetRandomInstrumentTool>(),
        get<GetTechAnalysisTool>(),
        get<GetInstrumentCandlesTool>(),
        get<GetUserPositionsTool>(),
        get<GetFavouriteInstrumentsTool>(),
        get<GetInstrumentByNameTool>(),
        get<GetNoteTool>(),
        get<NewNoteTool>()
    )

    fun getDeclarations(): List<Declaration> = executors.map { it.declaration }

    fun getTool(name: String?): GeminiToolExecutor? = executors.find { it.name == name }
}