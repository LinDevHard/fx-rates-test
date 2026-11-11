package com.fxrates.feature.rates.details

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.fxrates.feature.rates.details.RateDetailsUiState.Change
import com.fxrates.feature.rates.details.RateDetailsUiState.Chart
import com.fxrates.feature.rates.details.RateDetailsUiState.Content
import com.fxrates.feature.rates.details.model.HistoryItem
import com.fxrates.feature.rates.model.Trend

internal class RateDetailsUiStatePreviewProvider : PreviewParameterProvider<RateDetailsUiState> {

    override val values: Sequence<RateDetailsUiState> = sequenceOf(
        RateDetailsUiState(
            title = "USD / EUR",
            content = Content.Data(
                rate = "1 USD = 0.8916 EUR",
                date = "2026-11-02",
                change = Change.Value(
                    period = "Change 2026-10-03 – 2026-11-02",
                    absolute = "-0.0035",
                    percent = "-0.39%",
                    trend = Trend.Down,
                ),
                chart = Chart.Line(
                    points = listOf(1f, 0.8f, 0.9f, 0.55f, 0.6f, 0.3f, 0.4f, 0.15f, 0f, 0.1f),
                    min = "0.8910",
                    max = "0.8951",
                    trend = Trend.Down,
                ),
                history = listOf(
                    HistoryItem(date = "2026-11-02", rate = "0.8916"),
                    HistoryItem(date = "2026-11-01", rate = "0.8921"),
                    HistoryItem(date = "2026-10-03", rate = "0.8951"),
                ),
            ),
        ),
        RateDetailsUiState(
            title = "USD / EUR",
            content = Content.Data(
                rate = "1 USD = 0.8916 EUR",
                date = "2026-11-02",
                change = Change.NotEnoughData,
                chart = Chart.NotEnoughData,
                history = listOf(HistoryItem(date = "2026-11-02", rate = "0.8916")),
            ),
        ),
        RateDetailsUiState(title = "USD / EUR", content = Content.Loading),
        RateDetailsUiState(title = "USD / EUR", content = Content.Error),
        RateDetailsUiState(title = "USD / EUR", content = Content.NoData),
    )

}
