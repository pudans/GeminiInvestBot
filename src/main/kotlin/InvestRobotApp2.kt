package ru.pudans.investrobot

import agent.InvestAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class InvestRobotApp2(
) : KoinComponent {

    private val agent: InvestAgentProvider by inject()


    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

            val agent = agent.provideAgent(
//                onToolCallEvent = { println(it) },
//                onErrorEvent = { println(it) },
                onAssistantMessage = {
                    println("onAssistantMessage: $it")
                    "User unput is null"
                }
            )

            val result =
                agent.run("Get 2-3 random instruments and analyse them. Provide the results")

            println(result)

        }
    }
}