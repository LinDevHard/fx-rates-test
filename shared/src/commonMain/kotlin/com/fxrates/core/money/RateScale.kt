package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode

object RateScale {

    private const val MIDDLE = 0.5f

    /**
     * Places every rate between the lowest (`0`) and the highest (`1`) rate of the list. The result is
     * a position for drawing, not an amount, so it is a [Float]. When all rates are equal, each sits at `0.5`.
     */
    fun normalize(rates: List<BigDecimal>): List<Float> {
        val min = rates.minOrNull() ?: return emptyList()
        val max = rates.max()
        val range = max - min
        if (range.isZero()) return rates.map { MIDDLE }
        return rates.map { rate -> (rate - min).divide(range, POSITION_DECIMAL_MODE).floatValue(exactRequired = false) }
    }

    private val POSITION_DECIMAL_MODE = DecimalMode(
        decimalPrecision = 10,
        roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
    )

}
