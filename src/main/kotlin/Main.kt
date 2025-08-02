package ru.pudans.investrobot

import ru.pudans.investrobot.di.initKoin

fun main() {

    initKoin()

//    InvestRobotApp2().invoke()

    // Option 2: Use the original Gemini-based approach
//    val config = GeminiInvestRobotApp.Config(
//        accountId = realAccountId,
//        target = GeminiInvestRobotApp.TargetInstrument.Random
//    )


    GeminiInvestRobotApp3().invoke()
}