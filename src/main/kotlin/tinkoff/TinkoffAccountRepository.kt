package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pudans.investrobot.models.Account
import ru.pudans.investrobot.repository.AccountRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.tinkoff.piapi.contract.v1.AccountStatus
import ru.tinkoff.piapi.contract.v1.MoneyValue

class TinkoffAccountRepository(
    private val api: TinkoffInvestApi
) : AccountRepository {

    override suspend fun openAccount(name: String): Result<Account> = withContext(Dispatchers.IO) {
        runCatching {
            api.sandboxService.openAccountSync(name).let {
                Account(
                    id = it,
                    name = ""
                )
            }
        }
    }

    override suspend fun getAccounts(): Result<List<Account>> = withContext(Dispatchers.IO) {
        runCatching {
            api.userService.getAccountsSync(AccountStatus.ACCOUNT_STATUS_ALL).map {
                Account(
                    id = it.id,
                    name = it.name
                )
            }
        }
    }

    override suspend fun addMoney(accountId: String, units: Long): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val moneyValue = MoneyValue.newBuilder().setUnits(units).build()
                api.sandboxService.payInSync(accountId, moneyValue)
                Unit
            }
        }
}