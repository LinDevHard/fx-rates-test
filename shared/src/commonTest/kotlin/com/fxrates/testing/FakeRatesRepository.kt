package com.fxrates.testing

import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.RatePoint
import com.fxrates.domain.repository.RatesRepository

class FakeRatesRepository : RatesRepository {

    var latestRates: Result<List<CurrencyRate>> = Result.success(emptyList())

    var rateHistory: Result<List<RatePoint>> = Result.success(emptyList())

    val requestedBases = mutableListOf<String>()

    /** Pairs of base to code. */
    val requestedHistories = mutableListOf<Pair<String, String>>()

    override suspend fun getLatestRates(base: String): List<CurrencyRate> {
        requestedBases += base
        return latestRates.getOrThrow()
    }

    override suspend fun getRateHistory(base: String, code: String): List<RatePoint> {
        requestedHistories += base to code
        return rateHistory.getOrThrow()
    }

}
