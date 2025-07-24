package ru.pudans.investrobot.models

data class Share(
    val figi: String,
    val ticker: String,
    val name: String,
    val uid: String,
    val currency: String,
    val lot: Int
) 