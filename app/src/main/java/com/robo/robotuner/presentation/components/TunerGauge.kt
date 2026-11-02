package com.robo.robotuner.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.robo.robotuner.ui.theme.AccentGreen
import com.robo.robotuner.ui.theme.AccentOrange
import com.robo.robotuner.ui.theme.AccentRed
import com.robo.robotuner.ui.theme.DarkCard
import com.robo.robotuner.ui.theme.TextMuted
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TunerGauge(
    cents: Double,
    isInTune: Boolean,
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val animatedCents by animateFloatAsState(
        targetValue = cents.toFloat().coerceIn(-50f, 50f),
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 150f),
        label = "cents"
    )

    val indicatorColor = when {
        !isActive -> TextMuted
        isInTune -> AccentGreen
        animatedCents < -5 -> AccentRed
        animatedCents > 5 -> AccentOrange
        else -> AccentGreen
    }

    Canvas(modifier = modifier.size(280.dp)) {
        val strokeWidth = 12.dp.toPx()
        val padding = 24.dp.toPx()
        val arcSize = Size(size.width - padding * 2, size.height - padding * 2)
        val topLeft = Offset(padding, padding)

        drawArcBackground(topLeft, arcSize, strokeWidth)
        drawTicks(size.width / 2, size.height / 2, arcSize.width / 2 - strokeWidth)

        if (isActive) {
            drawIndicator(
                centerX = size.width / 2,
                centerY = size.height / 2,
                radius = arcSize.width / 2 - strokeWidth / 2,
                cents = animatedCents,
                color = indicatorColor,
                isInTune = isInTune
            )
        }
    }
}

private fun DrawScope.drawArcBackground(topLeft: Offset, arcSize: Size, strokeWidth: Float) {
    drawArc(
        color = DarkCard,
        startAngle = 135f,
        sweepAngle = 270f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )

    drawArc(
        color = AccentGreen.copy(alpha = 0.15f),
        startAngle = 261f,
        sweepAngle = 18f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
    )
}

private fun DrawScope.drawTicks(centerX: Float, centerY: Float, radius: Float) {
    val tickCount = 11
    val startAngle = 135.0
    val sweepAngle = 270.0

    for (i in 0 until tickCount) {
        val angle = Math.toRadians(startAngle + (sweepAngle / (tickCount - 1)) * i)
        val isCenterTick = i == tickCount / 2
        val isMainTick = i % 2 == 0

        val innerRadius = radius - when {
            isCenterTick -> 25.dp.toPx()
            isMainTick -> 18.dp.toPx()
            else -> 10.dp.toPx()
        }
        val outerRadius = radius - 4.dp.toPx()

        val startX = centerX + innerRadius * cos(angle).toFloat()
        val startY = centerY + innerRadius * sin(angle).toFloat()
        val endX = centerX + outerRadius * cos(angle).toFloat()
        val endY = centerY + outerRadius * sin(angle).toFloat()

        val tickColor = when {
            isCenterTick -> AccentGreen
            else -> TextMuted
        }

        drawLine(
            color = tickColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = if (isCenterTick) 3.dp.toPx() else 2.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

private fun DrawScope.drawIndicator(
    centerX: Float,
    centerY: Float,
    radius: Float,
    cents: Float,
    color: Color,
    isInTune: Boolean
) {
    val normalizedCents = cents / 50f
    val angle = Math.toRadians(270.0 + normalizedCents * 135.0)

    val indicatorLength = radius - 30.dp.toPx()
    val endX = centerX + indicatorLength * cos(angle).toFloat()
    val endY = centerY + indicatorLength * sin(angle).toFloat()

    drawLine(
        color = color,
        start = Offset(centerX, centerY),
        end = Offset(endX, endY),
        strokeWidth = 4.dp.toPx(),
        cap = StrokeCap.Round
    )

    val dotRadius = if (isInTune) 14.dp.toPx() else 10.dp.toPx()
    drawCircle(
        color = color,
        radius = dotRadius,
        center = Offset(endX, endY)
    )

    drawCircle(
        color = DarkCard,
        radius = 8.dp.toPx(),
        center = Offset(centerX, centerY)
    )
}
