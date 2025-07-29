package ru.pudans.investrobot.models

import kotlinx.serialization.Serializable
import java.time.Instant
import java.util.UUID

@Serializable
enum class StopOrderDirection {
    BUY, SELL
}

@Serializable
enum class StopOrderType {
    TAKE_PROFIT,
    STOP_LOSS,
    STOP_LIMIT
}

@Serializable
enum class StopOrderExpirationType {
    GOOD_TILL_CANCEL,
    GOOD_TILL_DATE
}

@Serializable
enum class TakeProfitType {
    REGULAR,
    TRAILING
}

@Serializable
enum class TrailingValueType {
    ABSOLUTE,
    RELATIVE
}

@Serializable
data class TrailingData(
    val indent: Double?,
    val indentType: TrailingValueType?,
    val spread: Double?,
    val spreadType: TrailingValueType?
)

data class PostOrderRequest(
    val instrumentId: String,
    val quantity: Long,
    val price: Double?,
    val stopPrice: Double,
    val direction: StopOrderDirection,
    val accountId: String,
    val type: StopOrderType,
    val expirationType: StopOrderExpirationType,
    val takeProfitType: TakeProfitType,
    val trailingData: TrailingData?,
    val expireDate: Instant?,
    val orderId: UUID?
) 