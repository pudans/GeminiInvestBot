package ru.pudans.investrobot.tinkoff.api

import ru.pudans.investrobot.secrets.GetSecretUseCase
import ru.pudans.investrobot.secrets.SecretKey
import ru.tinkoff.piapi.core.InvestApi

class TinkoffInvestApi(
    private val getSecret: GetSecretUseCase
) {

    private val api: InvestApi by lazy {
        val prodKey = getSecret(SecretKey.TINKOFF_ACCOUNT_BOT_EXPERIMENT)
        InvestApi.create(prodKey)
    }

    val sandboxService = api.sandboxService
    val userService = api.userService
    val marketDataService = api.marketDataService
    val operationsService = api.operationsService
    val instrumentsService = api.instrumentsService
    val marketDataStreamService = api.marketDataStreamService
    val operationsStreamService = api.operationsStreamService
    val ordersService = api.ordersService
    val signalService = api.signalService
    val stopOrdersService = api.stopOrdersService
}