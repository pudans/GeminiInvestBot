plugins {
    kotlin("jvm") version "2.1.21"
    kotlin("plugin.serialization") version "2.1.21"
    id("io.ktor.plugin") version "3.2.1"
}

group = "ru.pudans.investrobot"
version = "0.0.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
    maven("https://packages.jetbrains.team/maven/p/skija/maven")
    maven("https://packages.jetbrains.team/maven/p/grazi/grazie-platform-public")
}

dependencies {

    // Tinkoff API
    implementation("ru.tinkoff.piapi:java-sdk-core:1.31")
    implementation("ru.tinkoff.piapi:java-sdk-strategy:1.31")
    implementation("ru.tinkoff.piapi:java-sdk-storage-csv:1.31")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.2")

    // Koin
    implementation("io.insert-koin:koin-core:4.1.0")

    // Ktor
    implementation("io.ktor:ktor-client-java:3.2.1")
    implementation("io.ktor:ktor-client-core:3.2.1")
    implementation("io.ktor:ktor-client-content-negotiation:3.2.1")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.2.1")

    // Telegram Bot API
    implementation("io.github.kotlin-telegram-bot.kotlin-telegram-bot:telegram:6.3.0")
}

kotlin {
    jvmToolchain(19)
}

application {
    mainClass.set("ru.pudans.investrobot.MainKt")
}