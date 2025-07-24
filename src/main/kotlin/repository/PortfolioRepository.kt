package ru.pudans.investrobot.repository

import ru.pudans.investrobot.models.Portfolio
import ru.pudans.investrobot.models.Position

interface PortfolioRepository {
    suspend fun getPositions(accountId: String): Result<List<Position>>
    suspend fun getPortfolio(accountId: String): Result<Portfolio>
}