package ru.pudans.investrobot.repository

import ru.pudans.investrobot.models.StopOrderDirection
import ru.pudans.investrobot.models.StopOrderExpirationType
import ru.pudans.investrobot.models.StopOrderType
import ru.pudans.investrobot.models.TakeProfitType
import ru.pudans.investrobot.models.TrailingData

interface PostOrderRepository {
    suspend fun postOrder(
        instrumentId: String,
        quantity: Long,
        price: Double,
        stopPrice: Double,
        direction: StopOrderDirection,
        accountId: String,
        stopOrderType: StopOrderType,
        expirationType: StopOrderExpirationType,
        takeProfitType: TakeProfitType,
        trailingData: TrailingData?,
    ): Result<String>
} 