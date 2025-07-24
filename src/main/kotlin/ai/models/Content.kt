package ru.pudans.investrobot.ai.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Content(
    val parts: List<Part>? = null,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null,
    val fileData: FileData? = null,
    val functionCall: FunctionCall? = null,
    val functionResponse: FunctionResponse? = null
)

@Serializable
data class InlineData(
    val mimeType: String? = null,
    val data: String? = null
)

@Serializable
data class FileData(
    val mimeType: String? = null,
    val fileUri: String? = null
)

@Serializable
data class FunctionCall(
    val id: String? = null,
    val name: String? = null,
    val args: Map<String, JsonElement>? = null
)

@Serializable
data class FunctionResponse(
    val id: String? = null,
    val name: String? = null,
    val response: Map<String, JsonElement>? = null
)