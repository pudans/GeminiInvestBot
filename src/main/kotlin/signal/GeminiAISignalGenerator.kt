package ru.pudans.investrobot.signal
//
//import ru.pudans.investrobot.ai.GeminiClient
//import ru.pudans.investrobot.ai.models.Content
//import ru.pudans.investrobot.ai.models.GeminiModel
//import ru.pudans.investrobot.ai.models.Part
//import ru.pudans.investrobot.models.CandleInterval
//
///**
// * AI-powered signal generator using Gemini for advanced market analysis
// */
//class GeminiAISignalGenerator(
//    private val geminiClient: GeminiClient
//) : SignalGenerator {
//
//    override val name: String = "GeminiAISignal"
//    override val priority: Int = 3 // Higher priority due to sophisticated analysis
//
//    override suspend fun generateSignal(context: SignalContext): Result<GeneratedSignal> {
//        return try {
//            val prompt = buildAnalysisPrompt(context)
//            val response = geminiClient.generateContent(
//                model = GeminiModel.FLASH_2_0,
//                temperature = 0.1, // Low temperature for more consistent analysis
//                contents = listOf(
//                    Content(
//                        parts = listOf(Part(text = prompt)),
//                        role = "user"
//                    )
//                )
//            )
//
//            val analysisText = response.getOrThrow().parts?.first()?.text
//                ?: throw RuntimeException("No response from Gemini")
//
//            val signal = parseGeminiResponse(analysisText, context)
//            Result.success(signal)
//
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
//
//    private fun buildAnalysisPrompt(context: SignalContext): String {
//        val candleData = buildCandleDataString(context)
//
//        return """
//            Analyze the following market data and provide a trading recommendation for ${context.instrument.name} (${context.instrument.figi}).
//
//            Current Price: ${context.currentPrice}
//            Current Volume: ${context.volume}
//
//            Multi-timeframe Candle Data:
//            $candleData
//
//            Please provide your analysis in the following JSON format:
//            {
//                "recommendation": "BUY|SELL|HOLD|WAIT",
//                "confidence": "LOW|MEDIUM|HIGH|VERY_HIGH",
//                "reasoning": "Detailed explanation of your analysis",
//                "entryPrice": <suggested_entry_price>,
//                "targetPrice": <target_price>,
//                "stopLoss": <stop_loss_price>,
//                "probability": <probability_0_to_1>,
//                "primaryTimeframe": "INTERVAL_1_HOUR"
//            }
//
//            Consider:
//            1. Multi-timeframe trend alignment
//            2. Support/resistance levels
//            3. Volume confirmation
//            4. Risk/reward ratio
//            5. Japanese candlestick patterns
//            6. Market momentum
//
//            Be conservative and prioritize capital preservation. Only recommend BUY/SELL with HIGH confidence if there are clear, multiple confirming signals across timeframes.
//        """.trimIndent()
//    }
//
//    private fun buildCandleDataString(context: SignalContext): String {
//        return context.multiTimeframeCandles.entries
//            .sortedByDescending { it.key.ordinal } // Start with higher timeframes
//            .joinToString("\n\n") { (interval, candles) ->
//                val recentCandles = candles.takeLast(10) // Last 10 candles for each timeframe
//                """
//                $interval (Last ${recentCandles.size} candles):
//                ${
//                    recentCandles.joinToString("\n") { candle ->
//                        "O:${candle.open} H:${candle.high} L:${candle.low} C:${candle.close} V:${candle.volume}"
//                    }
//                }
//                """.trimIndent()
//            }
//    }
//
//    private fun parseGeminiResponse(analysisText: String, context: SignalContext): GeneratedSignal {
//        // Try to extract JSON from the response
//        val jsonMatch = Regex("""\{[^}]*\}""").find(analysisText)
//
//        return if (jsonMatch != null) {
//            try {
//                parseJSONResponse(jsonMatch.value, analysisText, context)
//            } catch (e: Exception) {
//                // Fallback to text parsing
//                parseTextResponse(analysisText, context)
//            }
//        } else {
//            parseTextResponse(analysisText, context)
//        }
//    }
//
//    private fun parseJSONResponse(json: String, fullText: String, context: SignalContext): GeneratedSignal {
//        // In a real implementation, you'd use a proper JSON parser
//        // This is a simplified version for demonstration
//
//        val recommendation = extractValue(json, "recommendation") ?: "HOLD"
//        val confidence = extractValue(json, "confidence") ?: "LOW"
//        val reasoning = extractValue(json, "reasoning") ?: fullText.take(200)
//        val entryPrice = extractValue(json, "entryPrice")?.toDoubleOrNull() ?: context.currentPrice
//        val targetPrice = extractValue(json, "targetPrice")?.toDoubleOrNull()
//        val stopLoss = extractValue(json, "stopLoss")?.toDoubleOrNull()
//        val probability = extractValue(json, "probability")?.toDoubleOrNull()
//
//        return GeneratedSignal(
//            name = name,
//            result = parseSignalResult(recommendation),
//            confidence = parseConfidence(confidence),
//            reasoning = reasoning,
//            entryPrice = entryPrice,
//            targetPrice = targetPrice,
//            stopLossPrice = stopLoss,
//            timeframe = CandleInterval.INTERVAL_1_HOUR,
//            probability = probability
//        )
//    }
//
//    private fun parseTextResponse(analysisText: String, context: SignalContext): GeneratedSignal {
//        // Fallback text parsing for when JSON isn't provided
//        val text = analysisText.uppercase()
//
//        val result = when {
//            text.contains("BUY") && text.contains("STRONG") -> SignalResult.BUY
//            text.contains("SELL") && text.contains("STRONG") -> SignalResult.SELL
//            text.contains("BUY") -> SignalResult.BUY
//            text.contains("SELL") -> SignalResult.SELL
//            text.contains("WAIT") -> SignalResult.WAIT
//            else -> SignalResult.HOLD
//        }
//
//        val confidence = when {
//            text.contains("VERY HIGH") || text.contains("STRONG") -> SignalConfidence.VERY_HIGH
//            text.contains("HIGH") -> SignalConfidence.HIGH
//            text.contains("MEDIUM") -> SignalConfidence.MEDIUM
//            else -> SignalConfidence.LOW
//        }
//
//        return GeneratedSignal(
//            name = name,
//            result = result,
//            confidence = confidence,
//            reasoning = analysisText.take(500),
//            entryPrice = context.currentPrice,
//            timeframe = CandleInterval.INTERVAL_1_HOUR
//        )
//    }
//
//    private fun extractValue(json: String, key: String): String? {
//        val pattern = """"$key":\s*"([^"]*)"""".toRegex()
//        return pattern.find(json)?.groupValues?.get(1)
//    }
//
//    private fun parseSignalResult(result: String): SignalResult {
//        return when (result.uppercase()) {
//            "BUY" -> SignalResult.BUY
//            "SELL" -> SignalResult.SELL
//            "WAIT" -> SignalResult.WAIT
//            else -> SignalResult.HOLD
//        }
//    }
//
//    private fun parseConfidence(confidence: String): SignalConfidence {
//        return when (confidence.uppercase()) {
//            "VERY_HIGH", "VERY HIGH" -> SignalConfidence.VERY_HIGH
//            "HIGH" -> SignalConfidence.HIGH
//            "MEDIUM" -> SignalConfidence.MEDIUM
//            else -> SignalConfidence.LOW
//        }
//    }
//}
