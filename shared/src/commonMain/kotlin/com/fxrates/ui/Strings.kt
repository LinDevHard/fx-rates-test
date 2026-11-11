package com.fxrates.ui

/**
 * Every user-facing string of the app. The app is English-only, so a constant is enough; a
 * string is never written inline in a composable.
 */
object Strings {

    const val RATES_TITLE = "Rates"
    const val RATES_SEARCH_HINT = "Search by code or name"
    const val RATES_NOTHING_FOUND = "Nothing found"
    const val ERROR_TITLE = "Could not load the rates"
    const val RETRY = "Retry"
    const val REFRESH = "Refresh"
    const val RATE_HISTORY_TITLE = "Last 30 days"
    const val NO_DATA = "No data for the period"
    const val NOT_ENOUGH_DATA = "Not enough data to calculate the change"
    const val BACK = "Back"

    fun ratesSubtitle(base: String, date: String): String = "1 $base · $date"

    fun rateDetailsTitle(code: String, base: String): String = "$code / $base"

    fun currentRate(base: String, rate: String, code: String): String = "1 $base = $rate $code"

    fun changePeriod(start: String, end: String): String = "Change $start – $end"

}
