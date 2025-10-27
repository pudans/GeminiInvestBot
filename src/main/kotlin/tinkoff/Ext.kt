@file:OptIn(ExperimentalTime::class)

package ru.pudans.investrobot.tinkoff

import com.google.protobuf.Timestamp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import ru.tinkoff.piapi.contract.v1.Quotation
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

val Quotation.price: Double
    get() = units.toDouble() + (nano.toDouble() / 1_000_000_000)

val Timestamp.instant: Instant
    get() = Instant.fromEpochSeconds(this.seconds)

val Long.instant: Instant
    get() = Instant.fromEpochSeconds(this)

val currentTime: Instant
    get() = Clock.System.now()

val Instant.toLocalDateTime: LocalDateTime
    get() = toLocalDateTime(TimeZone.of("Europe/Moscow"))