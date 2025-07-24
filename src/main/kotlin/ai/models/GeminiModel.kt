package ru.pudans.investrobot.ai.models

enum class GeminiModel(
    val rawName: String,
    val supportedInputTypes: List<DataType>,
    val supportedOutputTypes: List<DataType>
) {

    FLASH_2_0_IMAGE(
        rawName = "gemini-2.0-flash-exp-image-generation",
        supportedInputTypes = DataType.entries.toList(),
        supportedOutputTypes = listOf(DataType.TEXT, DataType.IMAGE)
    ),

    PRO_2_5(
        rawName = "gemini-2.5-pro-preview-03-25",
        supportedInputTypes = DataType.entries.toList(),
        supportedOutputTypes = listOf(DataType.TEXT)
    ),

    FLASH_2_5(
        rawName = "gemini-2.5-flash",
        supportedInputTypes = DataType.entries.toList(),
        supportedOutputTypes = listOf(DataType.TEXT)
    ),

    FLASH_2_0(
        rawName = "gemini-2.0-flash",
        supportedInputTypes = DataType.entries.toList(),
        supportedOutputTypes = listOf(DataType.TEXT)
    ),

    FLASH_2_0_LITE(
        rawName = "gemini-2.0-flash-lite",
        supportedInputTypes = DataType.entries.toList(),
        supportedOutputTypes = listOf(DataType.TEXT)
    )
}

val temperatures = listOf(
    2.0, 1.75, 1.5, 1.25, 1.0, 0.75, 0.5, 0.25, 0.0
)