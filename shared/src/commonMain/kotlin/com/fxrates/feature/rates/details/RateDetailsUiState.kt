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
         * @property change the change from the earliest to the latest point, or not enough data.
         * @property chart the rate over the period, drawn above the history.
         * @property history the rate of every day of the period, newest first.
         */
        data class Data(
            val rate: String,
            val date: String,
            val change: Change,
            val chart: Chart,
            val history: List<HistoryItem>,
        ) : Content()

    }

    @Immutable
    sealed class Change {

        /** Fewer than two points or an earliest rate of zero: the change is undefined. */
        data object NotEnoughData : Change()

        data class Value(
            val period: String,
            val absolute: String,
            val percent: String,
            val trend: Trend,
        ) : Change()

    }

    @Immutable
    sealed class Chart {

        /** Fewer than two points: there is no line to draw. */
        data object NotEnoughData : Chart()

        /**
         * @property points the rate of every day, oldest first, from `0` (the period [min]) to `1` (its [max]).
         * @property trend the change over the period; it colours the line.
         */
        data class Line(
            val points: List<Float>,
            val min: String,
            val max: String,
            val trend: Trend,
        ) : Chart()

    }

}
