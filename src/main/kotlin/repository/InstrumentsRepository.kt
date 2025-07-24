package ru.pudans.investrobot.repository

import ru.pudans.investrobot.models.Instrument

interface InstrumentsRepository {
    suspend fun getShares(): Result<List<Instrument>>
    suspend fun getBonds(): Result<List<Instrument>>
    suspend fun getFutures(): Result<List<Instrument>>
    suspend fun getEtfs(): Result<List<Instrument>>
    suspend fun getCurrencies(): Result<List<Instrument>>
    suspend fun getFavorites(): Result<List<Instrument>>
    suspend fun getInstrumentByTicker(ticker: String, classCode: String): Result<Instrument>
    suspend fun getInstrumentByName(name: String): Result<List<Instrument>>
}