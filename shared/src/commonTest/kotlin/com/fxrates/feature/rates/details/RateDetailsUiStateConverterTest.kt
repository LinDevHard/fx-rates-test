package com.fxrates.feature.rates.details

import com.fxrates.core.utils.Resource
import com.fxrates.feature.rates.details.RateDetailsUiState.Change
import com.fxrates.feature.rates.details.RateDetailsUiState.Content
import com.fxrates.feature.rates.details.mvi.RateDetailsStore
import com.fxrates.feature.rates.model.Trend
import com.fxrates.testing.ratePoint
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.datetime.LocalDate

class RateDetailsUiStateConverterTest : FunSpec({

    val converter = RateDetailsUiStateConverter()

    fun state(vararg points: Pair<LocalDate, String>): RateDetailsStore.State {
        val history = points.map { (date, rate) -> ratePoint(date = date, rate = rate) }
        return RateDetailsStore.State(base = "USD", code = "EUR", history = Resource.Data(history))
    }

    fun changeOf(from: String, to: String): Change {
        val uiState = converter.createUiState(
            state(LocalDate(2026, 10, 13) to from, LocalDate(2026, 11, 10) to to),
        )
        return uiState.content.shouldBeInstanceOf<Content.Data>().change
    }

    test("loading and error map to their states") {
        val loading = RateDetailsStore.State(base = "USD", code = "EUR", history = Resource.Loading)
        val error = loading.copy(history = Resource.Error(IllegalStateException()))

        converter.createUiState(loading).content shouldBe Content.Loading
        converter.createUiState(error).content shouldBe Content.Error
    }

    test("the title names the currency and the base") {
        converter.createUiState(state()).title shouldBe "EUR / USD"
    }

    test("no points give the no-data state") {
        converter.createUiState(state()).content shouldBe Content.NoData
    }

    test("one point shows the rate and says there is not enough data") {
        val content = converter.createUiState(state(LocalDate(2026, 11, 10) to "0.89"))
            .content.shouldBeInstanceOf<Content.Data>()

        content.rate shouldBe "1 USD = 0.8900 EUR"
        content.date shouldBe "2026-11-10"
        content.change shouldBe Change.NotEnoughData
        content.history.size shouldBe 1
    }

    test("the change over the period is signed and relative to the first point") {
        val uiState = converter.createUiState(
            state(
                LocalDate(2026, 10, 13) to "0.88",
                LocalDate(2026, 10, 20) to "0.885",
                LocalDate(2026, 11, 10) to "0.89",
            ),
        )

        uiState.content.shouldBeInstanceOf<Content.Data>().change shouldBe Change.Value(
            period = "Change 2026-10-13 – 2026-11-10",
            absolute = "+0.0100",
            percent = "+1.14%",
            trend = Trend.Up,
        )
    }

    test("a fall is negative") {
        val change = changeOf(from = "0.89", to = "0.88").shouldBeInstanceOf<Change.Value>()

        change.absolute shouldBe "-0.0100"
        change.percent shouldBe "-1.12%"
        change.trend shouldBe Trend.Down
    }

    test("an unchanged rate has no sign and is flat") {
        val change = changeOf(from = "0.88", to = "0.88").shouldBeInstanceOf<Change.Value>()

        change.absolute shouldBe "0.0000"
        change.percent shouldBe "0.00%"
        change.trend shouldBe Trend.Flat
    }

    test("a zero first rate gives not enough data") {
        changeOf(from = "0", to = "0.89") shouldBe Change.NotEnoughData
    }

    test("the history is newest first") {
        val content = converter.createUiState(
            state(
                LocalDate(2026, 10, 13) to "0.88",
                LocalDate(2026, 10, 20) to "0.885",
                LocalDate(2026, 11, 10) to "0.89",
            ),
        ).content.shouldBeInstanceOf<Content.Data>()

        content.history.map { item -> item.date } shouldBe listOf("2026-11-10", "2026-10-20", "2026-10-13")
        content.history.map { item -> item.rate } shouldBe listOf("0.8900", "0.8850", "0.8800")
    }

})
