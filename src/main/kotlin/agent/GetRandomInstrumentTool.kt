package ru.pudans.investrobot.agent

import ai.koog.agents.core.tools.Tool
import ai.koog.agents.core.tools.ToolArgs
import ai.koog.agents.core.tools.ToolDescriptor
import ai.koog.agents.core.tools.ToolResult
import kotlinx.serialization.Serializable
import org.koin.core.component.KoinComponent
import ru.pudans.investrobot.models.Instrument
import ru.pudans.investrobot.repository.InstrumentsRepository

class GetRandomInstrumentTool(
    val instrumentsRepository: InstrumentsRepository
) : Tool<GetRandomInstrumentTool.Args, GetRandomInstrumentTool.Result>(), KoinComponent {

    @Serializable
    class Args : ToolArgs

    @Serializable
    @JvmInline
    value class Result(
        val share: Instrument
    ) : ToolResult {
        override fun toStringDefault(): String {
            return "Random Instrument: ${share.name} (${share.figi}), InstrumentId: ${share.uid}, Type: ${share.type}"
        }
    }

    override val argsSerializer = Args.serializer()

    override val descriptor = ToolDescriptor(
        name = "GetRandomInstrumentTool",
        description = "Returns a random instrument from the available instruments in the instruments repository. Useful for getting a random stock for analysis or demonstration purposes.",
        requiredParameters = emptyList()
    )

    override suspend fun execute(args: Args): Result {
        val shares = instrumentsRepository.getShares().getOrThrow()
        val randomShare = shares.random()
        return Result(randomShare)
    }
} 