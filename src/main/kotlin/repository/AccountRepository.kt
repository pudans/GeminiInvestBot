package ru.pudans.investrobot.repository

import ru.pudans.investrobot.models.Account

interface AccountRepository {
    suspend fun openAccount(name: String): Result<Account>
    suspend fun getAccounts(): Result<List<Account>>
    suspend fun addMoney(accountId: String, units: Long): Result<Unit>
}