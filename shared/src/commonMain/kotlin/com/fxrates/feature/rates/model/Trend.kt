package com.fxrates.feature.rates.model

import com.fxrates.core.money.RateChange

enum class Trend {

    Up,
    Down,
    Flat,

}

/** A missing change and a change that is exactly zero are both [Trend.Flat]. */
fun RateChange?.toTrend(): Trend = when {
    this == null || absolute.isZero() -> Trend.Flat
    absolute.isPositive -> Trend.Up
    else -> Trend.Down
}
