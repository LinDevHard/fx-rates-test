package com.fxrates.feature.rates.details.model

import androidx.compose.runtime.Immutable

/** One row of the rate history. Every text field is already formatted for display. */
@Immutable
data class HistoryItem(
    val date: String,
    val rate: String,
)
