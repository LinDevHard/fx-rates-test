package com.fxrates.core.money

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import com.ionspin.kotlin.bignum.decimal.DecimalMode
import com.ionspin.kotlin.bignum.decimal.RoundingMode
import com.ionspin.kotlin.bignum.decimal.toBigDecimal

/**
 * The change between two rates of the same currency pair.
 *
 * @property absolute `current - previous`, in units of the quote currency.
 * @property percent the change relative to the previous rate; `1.5` means +1.5%. Computed in [BigDecimal],
 * rounded to 20 significant digits when the quotient does not terminate.
 */
data class RateChange(
    val absolute: BigDecimal,
    val percent: BigDecimal,
)

object RateChangeCalculator {

    /** Returns `null` when [previous] is zero and the relative change is undefined. */
    fun calculate(previous: BigDecimal, current: BigDecimal): RateChange? {
        if (previous.isZero()) return null
        val absolute = current - previous
        val percent = absolute.divide(previous, PERCENT_DECIMAL_MODE) * HUNDRED
        return RateChange(absolute = absolute, percent = percent)
    }

    private val PERCENT_DECIMAL_MODE = DecimalMode(
        decimalPrecision = 20,
        roundingMode = RoundingMode.ROUND_HALF_AWAY_FROM_ZERO,
    )

    private val HUNDRED = 100.toBigDecimal()

}
