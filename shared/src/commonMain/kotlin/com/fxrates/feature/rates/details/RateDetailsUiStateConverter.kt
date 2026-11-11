package com.fxrates.feature.rates.details

import com.fxrates.core.format.NumberFormatter
import com.fxrates.core.money.RateChangeCalculator
import com.fxrates.core.utils.Resource
import com.fxrates.domain.model.RatePoint
import com.fxrates.feature.rates.details.RateDetailsUiState.Change
import com.fxrates.feature.rates.details.RateDetailsUiState.Content
import com.fxrates.feature.rates.details.model.HistoryItem
import com.fxrates.feature.rates.details.mvi.RateDetailsStore
import com.fxrates.feature.rates.model.toTrend
import com.fxrates.ui.Strings
import me.tatarka.inject.annotations.Inject

@Inject
internal class RateDetailsUiStateConverter {

    fun createUiState(state: RateDetailsStore.State): RateDetailsUiState {
        return RateDetailsUiState(
            title = Strings.rateDetailsTitle(code = state.code, base = state.base),
            content = createContent(state),
        )
    }

    private fun createContent(state: RateDetailsStore.State): Content = when (val history = state.history) {
        is Resource.Loading -> {
            Content.Loading
        }
        is Resource.Error -> {
            Content.Error
        }
        is Resource.Data -> {
            createData(state = state, points = history.value)
        }
    }

    private fun createData(state: RateDetailsStore.State, points: List<RatePoint>): Content {
        val first = points.firstOrNull() ?: return Content.NoData
        val last = points.last()
        return Content.Data(
            rate = Strings.currentRate(
                base = state.base,
                rate = NumberFormatter.formatRate(last.rate),
                code = state.code,
            ),
            date = last.date.toString(),
            change = createChange(first = first, last = last),
            history = points.asReversed().map(::toItem),
        )
    }

    private fun createChange(first: RatePoint, last: RatePoint): Change {
        if (first == last) return Change.NotEnoughData
        val change = RateChangeCalculator.calculate(first.rate, last.rate) ?: return Change.NotEnoughData
        return Change.Value(
            period = Strings.changePeriod(start = first.date.toString(), end = last.date.toString()),
            absolute = NumberFormatter.formatSignedRate(change.absolute),
            percent = NumberFormatter.formatSignedPercent(change.percent),
            trend = change.toTrend(),
        )
    }

    private fun toItem(point: RatePoint): HistoryItem {
        return HistoryItem(date = point.date.toString(), rate = NumberFormatter.formatRate(point.rate))
    }

}
