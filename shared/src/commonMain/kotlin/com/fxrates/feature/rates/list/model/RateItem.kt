package com.fxrates.feature.rates.list.model

import androidx.compose.runtime.Immutable
import com.fxrates.feature.rates.model.Trend

/**
 * One row of the rates list. Every text field is already formatted for display.
 *
 * @property change the change since the previous business day; `null` when it is unknown.
 */
@Immutable
data class RateItem(
    val code: String,
    val name: String,
    val rate: String,
    val change: String?,
    val trend: Trend,
)
