package ru.pudans.investrobot.di

import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module
import ru.pudans.investrobot.ai.GeminiClient
import ru.pudans.investrobot.ai.GeminiClient2
import ru.pudans.investrobot.ai.tool.*
import ru.pudans.investrobot.logger.InvestRobotLogger
import ru.pudans.investrobot.repository.*
import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.telegram.TelegramBotManager
import ru.pudans.investrobot.tinkoff.*
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

    factoryOf(::GetTechAnalysisTool)
    factoryOf(::GetRandomInstrumentTool)
    factoryOf(::GetInstrumentCandlesTool)
    factoryOf(::GetUserPositionsTool)
    factoryOf(::GetInstrumentByNameTool)
    factoryOf(::GetFavouriteInstrumentsTool)
}

val telegramModule = module {
    singleOf(::TelegramBotManager)
}