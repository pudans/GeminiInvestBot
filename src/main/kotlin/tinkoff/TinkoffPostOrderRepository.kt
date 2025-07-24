package ru.pudans.investrobot.tinkoff

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.pudans.investrobot.logger.Logger
import ru.pudans.investrobot.models.StopOrderDirection
import ru.pudans.investrobot.models.StopOrderExpirationType
import ru.pudans.investrobot.models.StopOrderType
import ru.pudans.investrobot.models.TakeProfitType
import ru.pudans.investrobot.models.TrailingData
import ru.pudans.investrobot.models.TrailingValueType
import ru.pudans.investrobot.repository.PostOrderRepository
import ru.pudans.investrobot.tinkoff.api.TinkoffInvestApi
import ru.tinkoff.piapi.contract.v1.PostStopOrderRequest
import ru.tinkoff.piapi.contract.v1.Quotation
import java.util.UUID
import ru.tinkoff.piapi.contract.v1.StopOrderDirection as TinkoffStopOrderDirection
import ru.tinkoff.piapi.contract.v1.StopOrderExpirationType as TinkoffStopOrderExpirationType
import ru.tinkoff.piapi.contract.v1.StopOrderType as TinkoffStopOrderType
import ru.tinkoff.piapi.contract.v1.TakeProfitType as TinkoffTakeProfitType
import ru.tinkoff.piapi.contract.v1.TrailingValueType as TinkoffTrailingValueType

class TinkoffPostOrderRepository(
    private val api: TinkoffInvestApi,
    private val logger: Logger
) : PostOrderRepository {

    override suspend fun postOrder(
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
    ): Result<String> = withContext(Dispatchers.IO) {
        logger.info("Attempting to post stop order: instrument=$instrumentId, quantity=$quantity, price=$price, stopPrice=$stopPrice, direction=$direction")

        runCatching {
            val result = api.stopOrdersService.postStopOrderGoodTillCancelSync(
                instrumentId,
                quantity,
                parsePrice(price),
                parsePrice(stopPrice),
                mapDirection(direction),
                accountId,
                mapStopOrderType(stopOrderType),
//                mapExpirationType(expirationType),
//                mapTakeProfitType(takeProfitType),
//                mapTrailingData(trailingData),
//                null,
                UUID.randomUUID()
            )

            logger.info("Successfully posted stop order: instrument=$instrumentId, result=$result")
            result
        }.onFailure { throwable ->
            logger.error(
                "Failed to post stop order: instrument=$instrumentId, quantity=$quantity, price=$price",
                throwable
            )
        }
    }

    private fun parsePrice(price: Double): Quotation {
        val units = price.toLong()
        val nano = ((price - units) * 1_000_000_000).toInt()

        return Quotation.newBuilder()
            .setUnits(units)
            .setNano(nano)
            .build()
    }

    private fun mapDirection(direction: StopOrderDirection): TinkoffStopOrderDirection =
        when (direction) {
            StopOrderDirection.BUY -> TinkoffStopOrderDirection.STOP_ORDER_DIRECTION_BUY
            StopOrderDirection.SELL -> TinkoffStopOrderDirection.STOP_ORDER_DIRECTION_SELL
        }

    private fun mapStopOrderType(type: StopOrderType): TinkoffStopOrderType =
        when (type) {
            StopOrderType.TAKE_PROFIT -> TinkoffStopOrderType.STOP_ORDER_TYPE_TAKE_PROFIT
            StopOrderType.STOP_LOSS -> TinkoffStopOrderType.STOP_ORDER_TYPE_STOP_LOSS
            StopOrderType.STOP_LIMIT -> TinkoffStopOrderType.STOP_ORDER_TYPE_STOP_LIMIT
        }

    private fun mapExpirationType(expirationType: StopOrderExpirationType): TinkoffStopOrderExpirationType =
        when (expirationType) {
            StopOrderExpirationType.GOOD_TILL_CANCEL -> TinkoffStopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_CANCEL
            StopOrderExpirationType.GOOD_TILL_DATE -> TinkoffStopOrderExpirationType.STOP_ORDER_EXPIRATION_TYPE_GOOD_TILL_DATE
        }

    private fun mapTakeProfitType(takeProfitType: TakeProfitType): TinkoffTakeProfitType =
        when (takeProfitType) {
            TakeProfitType.REGULAR -> TinkoffTakeProfitType.TAKE_PROFIT_TYPE_REGULAR
            TakeProfitType.TRAILING -> TinkoffTakeProfitType.TAKE_PROFIT_TYPE_TRAILING
        }

    private fun mapTrailingValueType(trailingValueType: TrailingValueType): TinkoffTrailingValueType =
        when (trailingValueType) {
            TrailingValueType.ABSOLUTE -> TinkoffTrailingValueType.TRAILING_VALUE_ABSOLUTE
            TrailingValueType.RELATIVE -> TinkoffTrailingValueType.TRAILING_VALUE_RELATIVE
        }

    private fun mapTrailingData(trailingData: TrailingData?): PostStopOrderRequest.TrailingData? {
        trailingData ?: return null
        val builder = PostStopOrderRequest.TrailingData.newBuilder()

        trailingData.indent?.let {
            builder.setIndent(parsePrice(it))
        }

        trailingData.indentType?.let {
            builder.setIndentType(mapTrailingValueType(it))
        }

        trailingData.spread?.let {
            builder.setSpread(parsePrice(it))
        }

        trailingData.spreadType?.let {
            builder.setSpreadType(mapTrailingValueType(it))
        }

        return builder.build()
    }
} 