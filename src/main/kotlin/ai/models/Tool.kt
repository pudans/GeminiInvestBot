package ru.pudans.investrobot.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class Tool(
    val functionDeclarations: List<Declaration>
)

@Serializable
data class Declaration(
    val name: String,
    val description: String,
    val parameters: Parameters? = null
)

@Serializable
data class Parameters(
    val type: Type,
    val properties: Map<String, Property>,
    val required: List<String>
)

@Serializable
data class Property(
    val type: Type,
    val enum: List<String>? = null,
    val items: List<Item>? = null,
    val description: String
)

@Serializable
data class Item(
    val type: Type
)

enum class Type(val value: String) {
    OBJECT("object"),
    STRING("string"),
    INTEGER("integer"),
    NUMBER("number"),
    BOOLEAN("boolean"),
    ARRAY("array")
}
