package ru.pudans.investrobot.ai

import ru.pudans.investrobot.ai.models.Declaration
import ru.pudans.investrobot.ai.models.FunctionCall
import ru.pudans.investrobot.ai.models.FunctionResponse

interface GeminiToolExecutor {

    val name: String
    val declaration: Declaration

    suspend fun execute(args: FunctionCall): FunctionResponse
}