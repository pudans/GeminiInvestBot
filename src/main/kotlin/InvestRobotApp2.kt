package ru.pudans.investrobot

import agent.InvestAgentProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import ru.pudans.investrobot.repository.AccountRepository
import ru.pudans.investrobot.repository.PortfolioRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi

class InvestRobotApp2(
) : KoinComponent {

    private val agent: InvestAgentProvider by inject()

    private val api: TinkoffInvestApi by inject()
    private val repository: PortfolioRepository by inject()
    private val accountRepository: AccountRepository by inject()
    private val secrets: GetSecretUseCase by inject()

    operator fun invoke() = runBlocking {
        launch(context = Dispatchers.Default) {

            val account = accountRepository.getAccounts().getOrThrow().first()

            repository.getPositions(account.id).getOrThrow().let {
                println(it)
            }


//            val agent = agent.provideAgent(
////                onToolCallEvent = { println(it) },
////                onErrorEvent = { println(it) },
//                onAssistantMessage = {
//                    println("onAssistantMessage: $it")
//                    "User unput is null"
//                }
//            )
//
//            val result =
//                agent.run("Get 2-3 random instruments and analyse them. Provide the results")
//
//            println(result)

        }
    }
}