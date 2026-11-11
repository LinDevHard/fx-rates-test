package com.fxrates.feature.rates.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.fxrates.ui.theme.AppTheme

@Composable
internal fun Trend.color(): Color = when (this) {
    Trend.Up -> AppTheme.colors.positive
    Trend.Down -> AppTheme.colors.negative
    Trend.Flat -> AppTheme.colors.neutral
}
