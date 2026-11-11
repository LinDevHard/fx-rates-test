package com.fxrates.ui.chart

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fxrates.ui.theme.AppTheme

private const val FillAlpha = 0.25f

/**
 * A line through [points] spread evenly from left to right, with a fading fill under it. Draws nothing
 * for fewer than two points.
 *
 * @param points vertical positions, `0` at the bottom and `1` at the top.
 */
@Composable
fun LineChart(
    points: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    Spacer(
        modifier = modifier.drawWithCache {
            val stroke = strokeWidth.toPx()
            // The stroke is centred on the path, so the line is inset by half of it to stay inside the bounds.
            val top = stroke / 2
            val height = size.height - stroke
            val step = if (points.size > 1) size.width / (points.size - 1) else 0f
            val line = Path()
            val area = Path().apply { moveTo(0f, size.height) }
            points.forEachIndexed { index, point ->
                val x = index * step
                val y = top + height * (1f - point)
                if (index == 0) line.moveTo(x, y) else line.lineTo(x, y)
                area.lineTo(x, y)
            }
            area.lineTo(size.width, size.height)
            area.close()
            val fill = Brush.verticalGradient(listOf(color.copy(alpha = FillAlpha), Color.Transparent))
            onDrawBehind {
                if (points.size < 2) return@onDrawBehind
                drawPath(path = area, brush = fill)
                drawPath(
                    path = line,
                    color = color,
                    style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round),
                )
            }
        },
    )
}

@Preview
@Composable
private fun LineChartPreview() = AppTheme {
    LineChart(
        points = listOf(0f, 0.4f, 0.3f, 0.7f, 0.55f, 1f),
        color = AppTheme.colors.positive,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f),
    )
}
