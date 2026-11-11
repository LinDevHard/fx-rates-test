package com.fxrates.feature.rates.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import com.fxrates.feature.rates.details.RateDetailsUiState.Change
import com.fxrates.feature.rates.details.RateDetailsUiState.Content
import com.fxrates.feature.rates.details.model.HistoryItem
import com.fxrates.feature.rates.model.color
import com.fxrates.feature.rates.ui.Centered
import com.fxrates.feature.rates.ui.ErrorState
import com.fxrates.ui.Strings
import com.fxrates.ui.theme.AppTheme

@Composable
fun RateDetailsUiContent(
    component: RateDetailsUiComponent,
    modifier: Modifier = Modifier,
) {
    val state by component.state.collectAsState()
    RateDetailsUiContent(
        state = state,
        onRetry = component::onRetry,
        onBackClick = component::onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun RateDetailsUiContent(
    state: RateDetailsUiState,
    onRetry: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) = Column(
    modifier = modifier
        .fillMaxSize()
        .safeDrawingPadding(),
) {
    Header(title = state.title, onBackClick = onBackClick)
    when (val content = state.content) {
        is Content.Loading -> {
            Centered { CircularProgressIndicator() }
        }
        is Content.Error -> {
            ErrorState(onRetry = onRetry)
        }
        is Content.NoData -> {
            Centered { Text(Strings.NO_DATA, style = MaterialTheme.typography.bodyLarge) }
        }
        is Content.Data -> {
            DataList(content = content)
        }
    }
}

@Composable
private fun Header(
    title: String,
    onBackClick: () -> Unit,
) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
    verticalAlignment = Alignment.CenterVertically,
) {
    TextButton(onClick = onBackClick) {
        Text(Strings.BACK)
    }
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.weight(1f),
    )
}

@Composable
private fun DataList(content: Content.Data) = LazyColumn(modifier = Modifier.fillMaxSize()) {
    item {
        CurrentRate(rate = content.rate, date = content.date)
    }
    item {
        ChangeBlock(change = content.change)
    }
    item {
        Text(
            text = Strings.RATE_HISTORY_TITLE,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
        )
        HorizontalDivider()
    }
    items(items = content.history, key = HistoryItem::date) { item ->
        HistoryRow(item = item)
        HorizontalDivider()
    }
}

@Composable
private fun CurrentRate(
    rate: String,
    date: String,
) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
) {
    Text(rate, style = MaterialTheme.typography.headlineSmall)
    Text(date, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.neutral)
}

@Composable
private fun ChangeBlock(change: Change) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
) {
    when (change) {
        is Change.NotEnoughData -> {
            Text(Strings.NOT_ENOUGH_DATA, style = MaterialTheme.typography.bodyMedium)
        }
        is Change.Value -> {
            Text(change.period, style = MaterialTheme.typography.bodyMedium, color = AppTheme.colors.neutral)
            Text(change.absolute, style = MaterialTheme.typography.titleMedium, color = change.trend.color())
            Text(change.percent, style = MaterialTheme.typography.titleMedium, color = change.trend.color())
        }
    }
}

@Composable
private fun HistoryRow(item: HistoryItem) = Row(
    modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(item.date, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    Text(item.rate, style = MaterialTheme.typography.titleMedium)
}

@Preview
@Composable
private fun RateDetailsUiContentPreview(
    @PreviewParameter(RateDetailsUiStatePreviewProvider::class) state: RateDetailsUiState,
) = AppTheme {
    RateDetailsUiContent(
        state = state,
        onRetry = {},
        onBackClick = {},
    )
}
