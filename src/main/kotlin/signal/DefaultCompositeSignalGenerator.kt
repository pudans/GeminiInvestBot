package ru.pudans.investrobot.signal
//
//import kotlinx.coroutines.async
//import kotlinx.coroutines.awaitAll
//import kotlinx.coroutines.coroutineScope
//import ru.pudans.investrobot.models.CandleInterval
//
///**
// * Combines multiple signal generators to produce a final trading recommendation
// */
//class DefaultCompositeSignalGenerator(
//    private val signalGenerators: List<SignalGenerator>
//) : CompositeSignalGenerator {
//
//    override suspend fun generateCompositeSignal(context: SignalContext): Result<GeneratedSignal> {
//        return try {
//            val signals = collectAllSignals(context)
//            val composite = combineSignals(signals, context)
//            Result.success(composite)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    private suspend fun collectAllSignals(context: SignalContext): List<WeightedSignal> = coroutineScope {
//        // Run all signal generators in parallel for better performance
//        signalGenerators.map { generator ->
//            async {
//                try {
//                    val result = generator.generateSignal(context)
//                    WeightedSignal(
//                        signal = result.getOrNull(),
//                        generator = generator,
//                        error = result.exceptionOrNull()
//                    )
//                } catch (e: Exception) {
//                    WeightedSignal(
//                        signal = null,
//                        generator = generator,
//                        error = e
//                    )
//                }
//            }
//        }.awaitAll()
//    }
//
//    private fun combineSignals(weightedSignals: List<WeightedSignal>, context: SignalContext): GeneratedSignal {
//        val validSignals = weightedSignals.mapNotNull { it.signal }
//
//        if (validSignals.isEmpty()) {
//            return GeneratedSignal(
//                name = "",
//                result = SignalResult.WAIT,
//                confidence = SignalConfidence.LOW,
//                reasoning = "No valid signals available: ${
//                    weightedSignals.mapNotNull { it.error?.message }.joinToString("; ")
//                }",
//                timeframe = CandleInterval.INTERVAL_1_HOUR
//            )
//        }
//
//        // Weight signals by generator priority and confidence
//        val weightedVotes = validSignals.map { signal ->
//            val generator = weightedSignals.find { it.signal == signal }?.generator
//            val weight = calculateSignalWeight(signal, generator)
//            WeightedVote(signal, weight)
//        }
//
//        // Calculate vote totals
//        val buyVotes = weightedVotes.filter { it.signal.result == SignalResult.BUY }.sumOf { it.weight }
//        val sellVotes = weightedVotes.filter { it.signal.result == SignalResult.SELL }.sumOf { it.weight }
//        val holdVotes = weightedVotes.filter { it.signal.result == SignalResult.HOLD }.sumOf { it.weight }
//        val waitVotes = weightedVotes.filter { it.signal.result == SignalResult.WAIT }.sumOf { it.weight }
//
//        val totalVotes = buyVotes + sellVotes + holdVotes + waitVotes
//
//        // Determine final recommendation
//        val finalResult = when {
//            waitVotes > totalVotes * 0.5 -> SignalResult.WAIT
//            buyVotes > sellVotes && buyVotes > holdVotes -> SignalResult.BUY
//            sellVotes > buyVotes && sellVotes > holdVotes -> SignalResult.SELL
//            else -> SignalResult.HOLD
//        }
//
//        // Calculate composite confidence
//        val avgConfidence = calculateCompositeConfidence(weightedVotes, finalResult)
//
//        // Build composite reasoning
//        val reasoning =
//            buildCompositeReasoning(validSignals, finalResult, buyVotes, sellVotes, holdVotes, waitVotes, totalVotes)
//
//        // Calculate composite prices
//        val relevantSignals =
//            validSignals.filter { it.result == finalResult || it.result == SignalResult.BUY || it.result == SignalResult.SELL }
//        val avgEntryPrice =
//            relevantSignals.mapNotNull { it.entryPrice }.takeIf { it.isNotEmpty() }?.average() ?: context.currentPrice
//        val avgTargetPrice = relevantSignals.mapNotNull { it.targetPrice }.takeIf { it.isNotEmpty() }?.average()
//        val avgStopLoss = relevantSignals.mapNotNull { it.stopLossPrice }.takeIf { it.isNotEmpty() }?.average()
//        val avgProbability = relevantSignals.mapNotNull { it.probability }.takeIf { it.isNotEmpty() }?.average()
//
//        return GeneratedSignal(
//            name = "",
//            result = finalResult,
//            confidence = avgConfidence,
//            reasoning = reasoning,
//            entryPrice = avgEntryPrice,
//            targetPrice = avgTargetPrice,
//            stopLossPrice = avgStopLoss,
//            timeframe = CandleInterval.INTERVAL_1_HOUR,
//            probability = avgProbability,
//            riskRewardRatio = calculateRiskReward(avgEntryPrice, avgTargetPrice, avgStopLoss)
//        )
//    }
//
//    private fun calculateSignalWeight(signal: GeneratedSignal, generator: SignalGenerator?): Double {
//        val baseWeight = generator?.priority?.toDouble() ?: 1.0
//        val confidenceMultiplier = when (signal.confidence) {
//            SignalConfidence.VERY_HIGH -> 2.0
//            SignalConfidence.HIGH -> 1.5
//            SignalConfidence.MEDIUM -> 1.0
//            SignalConfidence.LOW -> 0.5
//        }
//        return baseWeight * confidenceMultiplier
//    }
//
//    private fun calculateCompositeConfidence(
//        weightedVotes: List<WeightedVote>,
//        finalResult: SignalResult
//    ): SignalConfidence {
//        val resultVotes = weightedVotes.filter { it.signal.result == finalResult }
//        if (resultVotes.isEmpty()) return SignalConfidence.LOW
//
//        val avgConfidenceScore = resultVotes.map { vote ->
//            when (vote.signal.confidence) {
//                SignalConfidence.VERY_HIGH -> 4.0
//                SignalConfidence.HIGH -> 3.0
//                SignalConfidence.MEDIUM -> 2.0
//                SignalConfidence.LOW -> 1.0
//            } * vote.weight
//        }.sum() / resultVotes.sumOf { it.weight }
//
//        return when {
//            avgConfidenceScore >= 3.5 -> SignalConfidence.VERY_HIGH
//            avgConfidenceScore >= 2.5 -> SignalConfidence.HIGH
//            avgConfidenceScore >= 1.5 -> SignalConfidence.MEDIUM
//            else -> SignalConfidence.LOW
//        }
//    }
//
//    private fun buildCompositeReasoning(
//        validSignals: List<GeneratedSignal>,
//        finalResult: SignalResult,
//        buyVotes: Double,
//        sellVotes: Double,
//        holdVotes: Double,
//        waitVotes: Double,
//        totalVotes: Double
//    ): String {
//        val votesSummary = "Vote distribution: BUY(${(buyVotes / totalVotes * 100).toInt()}%) " +
//                "SELL(${(sellVotes / totalVotes * 100).toInt()}%) " +
//                "HOLD(${(holdVotes / totalVotes * 100).toInt()}%) " +
//                "WAIT(${(waitVotes / totalVotes * 100).toInt()}%)"
//
//        val signalReasons = validSignals.groupBy { it.result }
//            .map { (result, signals) ->
//                "$result: ${signals.joinToString("; ") { it.reasoning.take(100) }}"
//            }
//            .joinToString(" | ")
//
//        return "$votesSummary. Final: $finalResult. Details: $signalReasons"
//    }
//
//    private fun calculateRiskReward(entryPrice: Double, targetPrice: Double?, stopLoss: Double?): Double? {
//        return if (targetPrice != null && stopLoss != null) {
//            val reward = kotlin.math.abs(targetPrice - entryPrice)
//            val risk = kotlin.math.abs(entryPrice - stopLoss)
//            if (risk > 0) reward / risk else null
//        } else null
//    }
//
//    private data class WeightedSignal(
//        val signal: GeneratedSignal?,
//        val generator: SignalGenerator,
//        val error: Throwable?
//    )
//
//    private data class WeightedVote(
//        val signal: GeneratedSignal,
//        val weight: Double
//    )
//}
