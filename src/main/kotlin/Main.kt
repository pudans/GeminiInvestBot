package ru.pudans.investrobot

import ru.pudans.investrobot.di.initKoin

fun main() {

    initKoin()

    // Option 1: Use the new simplified Investment Analysis Agent
//    InvestRobotApp2().invoke()

    // Option 2: Use the original Gemini-based approach
//    val config = GeminiInvestRobotApp.Config(
//        accountId = realAccountId,
//        target = GeminiInvestRobotApp.TargetInstrument.Random
//    )
    InvestRobotApp2().invoke()


//    InvestRobotApp(realAccountId, "").invoke()
}