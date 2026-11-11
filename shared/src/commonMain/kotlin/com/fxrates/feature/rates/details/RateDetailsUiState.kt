package com.fxrates.feature.rates.details

import androidx.compose.runtime.Immutable
import com.fxrates.core.utils.Empty
import com.fxrates.feature.rates.details.model.HistoryItem
import com.fxrates.feature.rates.model.Trend

@Immutable
internal data class RateDetailsUiState(
    val title: String = String.Empty,
    val content: Content = Content.Loading,
) {

    @Immutable
    sealed class Content {

        data object Loading : Content()

        data object Error : Content()

        data object NoData : Content()

        /**
         * @property rate the latest rate, e.g. `1 USD = 0.8900 EUR`.
         * @property date the date of the latest rate.
         * @property history the rate of every day of the period, newest first.
         */
        data class Data(
            val rate: String,
            val date: String,
            val change: Change,
            val history: List<HistoryItem>,
        ) : Content()

    }

    @Immutable
    sealed class Change {

        data object NotEnoughData : Change()

        data class Value(
            val period: String,
            val absolute: String,
            val percent: String,
            val trend: Trend,
        ) : Change()

    }

}
