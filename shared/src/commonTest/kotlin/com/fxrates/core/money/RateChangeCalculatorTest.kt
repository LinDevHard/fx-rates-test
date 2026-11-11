package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class RateChangeCalculatorTest : FunSpec({

    test("an unchanged rate gives a zero change") {
        val change = RateChangeCalculator.calculate(previous = "0.9".toBigDecimal(), current = "0.9".toBigDecimal())

        change.shouldNotBeNull()
        change.absolute.isZero() shouldBe true
        change.percent.isZero() shouldBe true
    }

    test("the absolute change is the difference between the rates") {
        val change = RateChangeCalculator.calculate(
            previous = "157.25".toBigDecimal(),
            current = "157.9".toBigDecimal(),
        )

        change.shouldNotBeNull()
        change.absolute shouldBe "0.65".toBigDecimal()
    }

    test("a falling rate gives a negative percent") {
        val change = RateChangeCalculator.calculate(previous = "0.9".toBigDecimal(), current = "0.8".toBigDecimal())

        change.shouldNotBeNull()
        change.percent shouldBeLessThan "0".toBigDecimal()
    }

    test("the percent is relative to the previous rate") {
        val change = RateChangeCalculator.calculate(previous = "0.88".toBigDecimal(), current = "0.89".toBigDecimal())

        change.shouldNotBeNull()
        val rounded = change.percent.roundToDigitPositionAfterDecimalPoint(5, RoundingMode.ROUND_HALF_AWAY_FROM_ZERO)
        rounded shouldBeEqualComparingTo "1.13636".toBigDecimal()
    }

    test("a rate rising from 100 to 150 gives a fifty percent change") {
        val change = RateChangeCalculator.calculate(previous = "100".toBigDecimal(), current = "150".toBigDecimal())

        change.shouldNotBeNull()
        change.percent shouldBeEqualComparingTo "50".toBigDecimal()
    }

    test("a zero previous rate has no relative change") {
        RateChangeCalculator.calculate(previous = "0".toBigDecimal(), current = "1".toBigDecimal()).shouldBeNull()
    }

})
