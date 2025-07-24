package ru.pudans.investrobot.models

import kotlinx.serialization.Serializable

@Serializable
data class Instrument(
    val figi: String,
    val uid: String,
    val type: String,
    val name: String
)
