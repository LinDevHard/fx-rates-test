package com.fxrates.domain.model

import com.ionspin.kotlin.bignum.decimal.BigDecimal
import kotlinx.datetime.LocalDate

/**
 * The rate on one business day.
 *
 * @property rate how many units of the currency one unit of the base currency buys.
 */
data class RatePoint(
    val date: LocalDate,
    val rate: BigDecimal,
)
