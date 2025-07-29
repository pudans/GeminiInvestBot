package ru.pudans.investrobot.models

import kotlinx.serialization.Serializable

@Serializable
data class Account(
    val id: String,
    val name: String
)
