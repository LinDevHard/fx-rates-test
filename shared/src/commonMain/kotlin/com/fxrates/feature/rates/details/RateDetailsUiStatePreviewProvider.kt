package com.fxrates.feature.rates.details

import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.fxrates.feature.rates.details.RateDetailsUiState.Change
import com.fxrates.feature.rates.details.RateDetailsUiState.Content
import com.fxrates.feature.rates.details.model.HistoryItem
import com.fxrates.feature.rates.model.Trend

internal class RateDetailsUiStatePreviewProvider : PreviewParameterProvider<RateDetailsUiState> {

    override val values: Sequence<RateDetailsUiState> = sequenceOf(
        RateDetailsUiState(
            title = "EUR / USD",
            content = Content.Data(
                rate = "1 USD = 0.8916 EUR",
                date = "2026-11-02",
                change = Change.Value(
                    period = "Change 2026-10-03 – 2026-11-02",
                    absolute = "-0.0035",
                    percent = "-0.39%",
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
            title = "EUR / USD",
            content = Content.Data(
                rate = "1 USD = 0.8916 EUR",
                date = "2026-11-02",
                change = Change.NotEnoughData,
                history = listOf(HistoryItem(date = "2026-11-02", rate = "0.8916")),
            ),
        ),
        RateDetailsUiState(title = "EUR / USD", content = Content.Loading),
        RateDetailsUiState(title = "EUR / USD", content = Content.Error),
        RateDetailsUiState(title = "EUR / USD", content = Content.NoData),
    )

}
