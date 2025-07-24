package ru.pudans.investrobot.ai.models

import kotlinx.serialization.Serializable

@Serializable
data class Response(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: Content? = null
)