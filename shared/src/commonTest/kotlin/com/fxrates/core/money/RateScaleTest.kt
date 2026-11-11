package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.toBigDecimal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe

class RateScaleTest : FunSpec({

    fun rates(vararg values: String) = values.map { value -> value.toBigDecimal() }

    test("no rates give no positions") {
        RateScale.normalize(emptyList()).shouldBeEmpty()
    }

    test("the lowest rate is at zero and the highest at one") {
        RateScale.normalize(rates("0.88", "0.90", "0.89")) shouldBe listOf(0f, 1f, 0.5f)
    }

    test("equal rates sit in the middle") {
        RateScale.normalize(rates("0.88", "0.88")) shouldBe listOf(0.5f, 0.5f)
    }

    test("a single rate sits in the middle") {
        RateScale.normalize(rates("157.9")) shouldBe listOf(0.5f)
    }

})
