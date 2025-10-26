package ru.pudans.investrobot

import ru.pudans.investrobot.di.initKoin

fun main() {

    initKoin()

//    InvestRobotApp2().invoke()

    // Option 1: Use the original Gemini-based approach
//    val config = GeminiInvestRobotApp.Config(
//        accountId = realAccountId,
//        target = GeminiInvestRobotApp.TargetInstrument.Random
//    )
//    GeminiInvestRobotApp2().invoke()

    // Option 2: Use the new Signal-Based Trading approach (RECOMMENDED)
    SignalBasedTradingApp().invoke()
}