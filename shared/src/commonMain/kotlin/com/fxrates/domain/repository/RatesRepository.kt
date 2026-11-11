package com.fxrates.domain.repository

import com.fxrates.domain.model.CurrencyRate
import com.fxrates.domain.model.RatePoint

interface RatesRepository {

    /** The latest rates of every supported currency against [base], sorted by code. */
    suspend fun getLatestRates(base: String): List<CurrencyRate>

    /**
     * The rates of [code] against [base] for the last 30 calendar days, oldest first. Empty when
     * there is no data for that window.
     */
    suspend fun getRateHistory(base: String, code: String): List<RatePoint>

}
