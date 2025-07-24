package agent

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.agent.config.AIAgentConfig
import ai.koog.agents.core.dsl.builder.forwardTo
import ai.koog.agents.core.dsl.builder.strategy
import ai.koog.agents.core.dsl.extension.nodeExecuteMultipleTools
import ai.koog.agents.core.dsl.extension.nodeLLMCompressHistory
import ai.koog.agents.core.dsl.extension.nodeLLMRequestMultiple
import ai.koog.agents.core.dsl.extension.nodeLLMSendMultipleToolResults
import ai.koog.agents.core.dsl.extension.onAssistantMessage
import ai.koog.agents.core.dsl.extension.onMultipleToolCalls
import ai.koog.agents.core.environment.ReceivedToolResult
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.agents.ext.tool.ExitTool
import ai.koog.prompt.dsl.prompt
import ai.koog.prompt.executor.clients.openai.OpenAIModels
import ai.koog.prompt.executor.llms.all.simpleOpenAIExecutor
import ru.pudans.investrobot.GetSecretUseCase
import ru.pudans.investrobot.SecretKey
import ru.pudans.investrobot.agent.GetInstrumentCandles
import ru.pudans.investrobot.agent.GetRandomInstrumentTool
import ru.pudans.investrobot.agent.GetTechAnalysisTool
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.repository.MarketDataRepository

class InvestAgentProvider(
    private val getSecret: GetSecretUseCase,
    private val marketDataRepository: MarketDataRepository,
    private val instrumentsRepository: InstrumentsRepository
) {

    val title = "Investment Analysis Agent"
    val description =
        "AI agent that analyzes random financial instruments using technical analysis and provides investment recommendations"

    fun provideAgent(
//        onToolCallEvent: suspend (String) -> Unit,
//        onErrorEvent: suspend (String) -> Unit,
        onAssistantMessage: suspend (String) -> String,
    ): AIAgent<String, String> {

        val token = getSecret(SecretKey.OPENAI_API_KEY)
        val executor = simpleOpenAIExecutor(token)

        val toolRegistry = ToolRegistry {
            tool(GetRandomInstrumentTool(instrumentsRepository))
            tool(GetInstrumentCandles(marketDataRepository))
            tool(GetTechAnalysisTool(marketDataRepository))
            tool(ExitTool)
        }

        val strategy = strategy(
            name = "InvestmentAnalysisStrategy"
        ) {
            val nodeRequestLLM by nodeLLMRequestMultiple()
            val nodeAssistantMessage by node<String, String> { message -> onAssistantMessage(message) }
            val nodeExecuteToolMultiple by nodeExecuteMultipleTools(parallelTools = true)
            val nodeSendToolResultMultiple by nodeLLMSendMultipleToolResults()
            val nodeCompressHistory by nodeLLMCompressHistory<List<ReceivedToolResult>>()

            edge(nodeStart forwardTo nodeRequestLLM)

            edge(
                nodeRequestLLM forwardTo nodeExecuteToolMultiple
                        onMultipleToolCalls { true }
            )

            edge(
                nodeRequestLLM forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

            edge(nodeAssistantMessage forwardTo nodeRequestLLM)

            // Finish condition - if exit tool is called, go to nodeFinish with tool call result.
            edge(
                nodeExecuteToolMultiple forwardTo nodeFinish
                        onCondition { it.singleOrNull()?.tool == ExitTool.name }
                        transformed { it.single().result!!.toStringDefault() }
            )

            edge(
                (nodeExecuteToolMultiple forwardTo nodeCompressHistory)
                        onCondition { _ -> llm.readSession { prompt.messages.size > 100 } }
            )

            edge(nodeCompressHistory forwardTo nodeSendToolResultMultiple)

            edge(
                (nodeExecuteToolMultiple forwardTo nodeSendToolResultMultiple)
                        onCondition { _ -> llm.readSession { prompt.messages.size <= 100 } }
            )

            edge(
                (nodeSendToolResultMultiple forwardTo nodeExecuteToolMultiple)
                        onMultipleToolCalls { true }
            )

            edge(
                nodeSendToolResultMultiple forwardTo nodeAssistantMessage
                        transformed { it.first() }
                        onAssistantMessage { true }
            )

        }

        // Create agent config with proper prompt
        val agentConfig = AIAgentConfig(
            prompt = prompt(
                "InvestmentAnalysisAgent"
            ) {
                system(
                    """
You are MarketInsight AI, an expert investment analysis agent specializing in technical analysis of financial instruments.

Your task is to:
1. Get a random financial instrument using GetRandomInstrumentTool
2. Analyze the instrument using multiple timeframes of candle data with GetInstrumentCandles
3. Perform comprehensive technical analysis across all timeframes
4. Provide investment recommendations (BUY/SELL/HOLD/CAUTION)
5. If the instrument shows strong BUY signals, return the FIGI and detailed reasoning

Analysis Process:
- Get 1-minute, 5-minute, 15-minute, 1-hour, 4-hour, and daily candle data
- Use timestamps: current time minus appropriate periods for each timeframe
- Analyze candlestick patterns, trends, support/resistance levels
- Consider volume, volatility, and momentum indicators
- Weight higher timeframes more heavily than lower ones

For timestamps, use these calculations from current time:
- 1-min: last 50 minutes (3000 seconds ago to now)
- 5-min: last 250 minutes (15000 seconds ago to now)  
- 15-min: last 750 minutes (45000 seconds ago to now)
- 1-hour: last 50 hours (180000 seconds ago to now)
- 4-hour: last 200 hours (720000 seconds ago to now)
- Daily: last 50 days (4320000 seconds ago to now)

Output Format:
If BUY recommendation:
```
FIGI: [instrument_figi]
RECOMMENDATION: BUY
REASONING: [Detailed analysis explaining why this instrument is attractive for investment, including key technical signals, timeframe analysis, and risk/reward assessment]
```

If not BUY (SELL/HOLD/CAUTION):
```
RECOMMENDATION: [SELL/HOLD/CAUTION]
REASONING: [Brief explanation of why this instrument is not suitable for buying at this time]
```

Always provide clear, professional analysis based on solid technical analysis principles.
                    """.trimIndent()
                )
            },
            model = OpenAIModels.CostOptimized.O4Mini,
            maxAgentIterations = 50
        )

        // Create agent using constructor to avoid inline method issue
        val agent = AIAgent<String, String>(
            promptExecutor = executor,
            strategy = strategy,
            agentConfig = agentConfig,
            toolRegistry = toolRegistry
        )

        // Configure events if needed
        // Note: This might need adjustment based on actual Koog API
        return agent
    }
}