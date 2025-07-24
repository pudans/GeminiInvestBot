package ru.pudans.investrobot.di

import agent.InvestAgentProvider
import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pudans.investrobot.GetSecretUseCase
import ru.pudans.investrobot.ai.GeminiClient
import ru.pudans.investrobot.ai.ai.GeminiClient2
import ru.pudans.investrobot.ai.ai.GetInstrumentCandlesTool
import ru.pudans.investrobot.ai.tool.ai.tool.GetRandomInstrumentTool
import ru.pudans.investrobot.ai.tool.ai.tool.ai.tool.ai.tool.GetTechAnalysisTool
import ru.pudans.investrobot.logger.InvestRobotLogger
import ru.pudans.investrobot.repository.AccountRepository
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.repository.PortfolioRepository
import ru.pudans.investrobot.repository.PostOrderRepository
import ru.pudans.investrobot.repository.SignalRepository
import ru.pudans.investrobot.telegram.TelegramBotManager
import ru.pudans.investrobot.tinkoff.TinkoffAccountRepository
import ru.pudans.investrobot.tinkoff.TinkoffInstrumentsRepository
import ru.pudans.investrobot.tinkoff.TinkoffMarketDataManager
import ru.pudans.investrobot.tinkoff.TinkoffMarketDataRepository
import ru.pudans.investrobot.tinkoff.TinkoffPortfolioRepository
import ru.pudans.investrobot.tinkoff.TinkoffPostOrderRepository
import ru.pudans.investrobot.tinkoff.TinkoffSignalRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi

fun initKoin(): KoinApplication =
    startKoin {
        modules(
            listOf(
                commonModule,
                tinkoffModule,
                aiModule,
                telegramModule
            )
        )
    }

val commonModule = module {
    singleOf(::GetSecretUseCase)
    singleOf(::InvestRobotLogger)

    single {
        HttpClient(Java) {
            install(ContentNegotiation) {
                json(
                    Json {
                        prettyPrint = true
                        isLenient = true
                        ignoreUnknownKeys = true
                    }
                )
            }
        }
    }
}

val tinkoffModule = module {
    single { TinkoffInvestApi(get()) }
    single<AccountRepository> { TinkoffAccountRepository(get()) }
    single<MarketDataRepository> { TinkoffMarketDataRepository(get()) }
    single<PortfolioRepository> { TinkoffPortfolioRepository(get()) }
    single<SignalRepository> { TinkoffSignalRepository(get()) }
    single<InstrumentsRepository> { TinkoffInstrumentsRepository(get()) }
    single<PostOrderRepository> { TinkoffPostOrderRepository(get(), get()) }
    factory { TinkoffMarketDataManager(get(), get()) }
}

val aiModule = module {
    singleOf(::GeminiClient)
    singleOf(::GeminiClient2)
    singleOf(::InvestAgentProvider)

    factoryOf(::GetTechAnalysisTool)
    factoryOf(::GetRandomInstrumentTool)
    factoryOf(::GetInstrumentCandlesTool)
}

val telegramModule = module {
    singleOf(::TelegramBotManager)
}