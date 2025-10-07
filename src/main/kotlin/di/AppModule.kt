package ru.pudans.investrobot.di

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
import ru.pudans.investrobot.ai.GeminiClient
import ru.pudans.investrobot.ai.GeminiClient2
import ru.pudans.investrobot.ai.GeminiClient3
import ru.pudans.investrobot.ai.tool.GetFavouriteInstrumentsTool
import ru.pudans.investrobot.ai.tool.GetInstrumentByNameTool
import ru.pudans.investrobot.ai.tool.GetInstrumentCandlesTool
import ru.pudans.investrobot.ai.tool.GetNoteTool
import ru.pudans.investrobot.ai.tool.GetRandomInstrumentTool
import ru.pudans.investrobot.ai.tool.GetTechAnalysisTool
import ru.pudans.investrobot.ai.tool.GetUserPositionsTool
import ru.pudans.investrobot.ai.tool.NewNoteTool
import ru.pudans.investrobot.database.NotesRepository
import ru.pudans.investrobot.logger.InvestRobotLogger
import ru.pudans.investrobot.repository.AccountRepository
import ru.pudans.investrobot.repository.InstrumentsRepository
import ru.pudans.investrobot.repository.MarketDataRepository
import ru.pudans.investrobot.repository.PortfolioRepository
import ru.pudans.investrobot.repository.PostOrderRepository
import ru.pudans.investrobot.repository.SignalRepository
import ru.pudans.investrobot.secrets.GetSecretUseCase
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
                telegramModule,
                databaseModule
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
    singleOf(::GeminiClient3)

    factoryOf(::GetTechAnalysisTool)
    factoryOf(::GetRandomInstrumentTool)
    factoryOf(::GetInstrumentCandlesTool)
    factoryOf(::GetUserPositionsTool)
    factoryOf(::GetInstrumentByNameTool)
    factoryOf(::GetFavouriteInstrumentsTool)
    factoryOf(::GetNoteTool)
    factoryOf(::NewNoteTool)
}

val telegramModule = module {
    singleOf(::TelegramBotManager)
}

val databaseModule = module {
    singleOf(::NotesRepository)
}