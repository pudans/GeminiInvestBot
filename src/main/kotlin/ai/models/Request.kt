package ru.pudans.investrobot.ai.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Request(
    @SerialName("system_instruction")
    val systemInstruction: Content? = null,
    val contents: List<Content>? = null,
    val tools: List<Tool>? = null,
    val safetySettings: List<SafetySettings>? = null,
    val generationConfig: GenerationConfig? = null,
    @SerialName("tool_config")
    val toolConfig: ToolConfig? = null
)

@Serializable
data class SafetySettings(
    val category: String? = null,
    val threshold: String? = null
)

@Serializable
data class GenerationConfig(
    val temperature: Double? = null,
    val responseModalities: List<String>? = null
)

@Serializable
data class ToolConfig(
    val functionCallingConfig: FunctionCallingConfig,
)

@Serializable
data class FunctionCallingConfig(
    val mode: String,
    val allowedFunctionNames: List<String>? = null
)