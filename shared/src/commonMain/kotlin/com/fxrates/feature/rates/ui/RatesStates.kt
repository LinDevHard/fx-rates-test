package com.fxrates.feature.rates.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.fxrates.ui.Strings
import com.fxrates.ui.theme.AppTheme

@Composable
internal fun ErrorState(onRetry: () -> Unit) = Centered {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
    ) {
        Text(Strings.ERROR_TITLE, style = MaterialTheme.typography.bodyLarge)
        Button(onClick = onRetry) {
            Text(Strings.RETRY)
        }
    }
}

@Composable
internal fun Centered(content: @Composable () -> Unit) = Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center,
) {
    content.invoke()
}
