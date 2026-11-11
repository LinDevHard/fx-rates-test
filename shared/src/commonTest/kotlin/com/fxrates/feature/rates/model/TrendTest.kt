package com.fxrates.feature.rates.model

import com.fxrates.core.money.RateChange
import com.fxrates.core.money.RateChangeCalculator
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TrendTest : FunSpec({

    test("no change is flat") {
        val unknown: RateChange? = null

        unknown.toTrend() shouldBe Trend.Flat
        RateChange(absolute = 0.toBigDecimal(), percent = 0.toBigDecimal()).toTrend() shouldBe Trend.Flat
    }

    test("a rise is up") {
        RateChangeCalculator.calculate("0.88".toBigDecimal(), "0.89".toBigDecimal()).toTrend() shouldBe Trend.Up
    }

    test("a fall is down") {
        RateChangeCalculator.calculate("0.89".toBigDecimal(), "0.88".toBigDecimal()).toTrend() shouldBe Trend.Down
    }

})
