package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.AmortizationYear
import com.example.domain.CurrencyOption
import com.example.ui.theme.LocalEmiColors
import com.example.ui.theme.TabularNumberStyle
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun EmiDonutChart(
    principalRatio: Float,
    interestRatio: Float,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    strokeWidth: Dp = 14.dp,
    principalColor: Color = LocalEmiColors.current.principalColor,
    interestColor: Color = LocalEmiColors.current.interestColor,
    trackColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    centerTextColor: Color = MaterialTheme.colorScheme.onSurface,
    centerSubColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val animatedPrincipal by animateFloatAsState(
        targetValue = principalRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "principalSweep"
    )
    val animatedInterest by animateFloatAsState(
        targetValue = interestRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "interestSweep"
    )

    val interestPct = (animatedInterest * 100f).roundToInt()

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = strokeWidth.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(this.size.width - strokePx, this.size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Base track
            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx)
            )

            val gapDegrees = if (animatedPrincipal > 0.02f && animatedInterest > 0.02f) 6f else 0f
            val principalSweep = (animatedPrincipal * 360f - gapDegrees).coerceAtLeast(0f)
            val interestSweep = (animatedInterest * 360f - gapDegrees).coerceAtLeast(0f)

            if (principalSweep > 0f) {
                drawArc(
                    color = principalColor,
                    startAngle = -90f,
                    sweepAngle = principalSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }

            if (interestSweep > 0f) {
                drawArc(
                    color = interestColor,
                    startAngle = -90f + principalSweep + gapDegrees,
                    sweepAngle = interestSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${interestPct}%",
                style = MaterialTheme.typography.titleLarge.merge(TabularNumberStyle),
                color = centerTextColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Interest",
                style = MaterialTheme.typography.labelSmall,
                color = centerSubColor
            )
        }
    }
}

@Composable
fun AmortizationStackedChart(
    yearlySchedule: List<AmortizationYear>,
    currency: CurrencyOption,
    useDeviceLocale: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (yearlySchedule.isEmpty()) return

    val emiColors = LocalEmiColors.current
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    val primaryText = MaterialTheme.colorScheme.onSurface
    val axisText = emiColors.chartAxisText
    val gridColor = emiColors.chartGridLine
    val selectionBg = emiColors.chartSelectionBg

    var selectedIndex by remember(yearlySchedule.size) {
        mutableIntStateOf(yearlySchedule.lastIndex.coerceAtLeast(0))
    }
    val safeSelectedIndex = selectedIndex.coerceIn(0, yearlySchedule.lastIndex)
    val selectedYear = yearlySchedule[safeSelectedIndex]

    val maxAnnualOutflow = remember(yearlySchedule) {
        yearlySchedule.maxOfOrNull {
            it.totalPrincipalPaid + it.totalExtraPaid + it.totalInterestPaid
        }?.coerceAtLeast(1.0) ?: 1.0
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Selected year readout pill
        Surface(
            color = surfaceVariant.copy(alpha = 0.65f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, emiColors.cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Year ${selectedYear.yearIndex}",
                    style = MaterialTheme.typography.labelLarge,
                    color = primaryText,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ChartLegendValue(
                        dotColor = emiColors.principalColor,
                        label = "Prin",
                        value = CurrencyFormatter.formatCompact(
                            selectedYear.totalPrincipalPaid + selectedYear.totalExtraPaid,
                            currency,
                            useDeviceLocale = useDeviceLocale
                        )
                    )
                    ChartLegendValue(
                        dotColor = emiColors.interestColor,
                        label = "Int",
                        value = CurrencyFormatter.formatCompact(
                            selectedYear.totalInterestPaid,
                            currency,
                            useDeviceLocale = useDeviceLocale
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
                .pointerInput(yearlySchedule) {
                    detectTapGestures { offset ->
                        val count = yearlySchedule.size.coerceAtLeast(1)
                        val tapped = ((offset.x / size.width) * count)
                            .toInt()
                            .coerceIn(0, count - 1)
                        selectedIndex = tapped
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val count = yearlySchedule.size
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.62f).coerceIn(4.dp.toPx(), 26.dp.toPx())
                val chartHeight = size.height - 6.dp.toPx()
                val usableHeight = chartHeight - 8.dp.toPx()
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)

                // Reference horizontal grid lines (50% and 100%)
                listOf(0.5f, 1.0f).forEach { fraction ->
                    val y = chartHeight - usableHeight * fraction
                    drawLine(
                        color = gridColor.copy(alpha = 0.55f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = dashEffect
                    )
                }

                // Baseline
                drawLine(
                    color = axisText.copy(alpha = 0.35f),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 1.dp.toPx()
                )

                yearlySchedule.forEachIndexed { index, year ->
                    val principalTotal = year.totalPrincipalPaid + year.totalExtraPaid
                    val interestTotal = year.totalInterestPaid
                    val total = principalTotal + interestTotal

                    val totalHeight = ((total / maxAnnualOutflow) * usableHeight).toFloat()
                    val principalHeight = if (total > 0) {
                        ((principalTotal / total) * totalHeight).toFloat()
                    } else 0f
                    val interestHeight = (totalHeight - principalHeight).coerceAtLeast(0f)

                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val isSelected = index == safeSelectedIndex
                    val alpha = if (isSelected) 1f else 0.72f

                    // Selection highlight column
                    if (isSelected) {
                        drawRoundRect(
                            color = selectionBg,
                            topLeft = Offset(index * slotWidth, 0f),
                            size = Size(slotWidth, chartHeight),
                            cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                        )
                    }

                    // Principal (bottom)
                    if (principalHeight > 0f) {
                        drawRoundRect(
                            color = emiColors.principalColor.copy(alpha = alpha),
                            topLeft = Offset(left, chartHeight - principalHeight),
                            size = Size(barWidth, principalHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    // Interest (stacked on top)
                    if (interestHeight > 0f) {
                        drawRoundRect(
                            color = emiColors.interestColor.copy(alpha = alpha),
                            topLeft = Offset(left, chartHeight - principalHeight - interestHeight),
                            size = Size(barWidth, interestHeight),
                            cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
                        )
                    }

                    if (isSelected && totalHeight > 0f) {
                        drawCircle(
                            color = primaryText,
                            radius = 2.5.dp.toPx(),
                            center = Offset(left + barWidth / 2f, (chartHeight - totalHeight - 5.dp.toPx()).coerceAtLeast(3.dp.toPx()))
                        )
                    }
                }
            }
        }

        // Axis labels (first, middle, last year)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Yr 1",
                style = MaterialTheme.typography.labelSmall,
                color = axisText
            )
            if (yearlySchedule.size > 2) {
                Text(
                    text = "Yr ${(yearlySchedule.size + 1) / 2}",
                    style = MaterialTheme.typography.labelSmall,
                    color = axisText,
                    textAlign = TextAlign.Center
                )
            }
            if (yearlySchedule.size > 1) {
                Text(
                    text = "Yr ${yearlySchedule.size}",
                    style = MaterialTheme.typography.labelSmall,
                    color = axisText
                )
            }
        }
    }
}

@Composable
private fun ChartLegendValue(
    dotColor: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(dotColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$label ",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

enum class PieSlice {
    PRINCIPAL,
    INTEREST
}

@Composable
fun LoanBreakdownPieChart(
    principalRatio: Float,
    interestRatio: Float,
    selectedSlice: PieSlice?,
    onSelectSlice: (PieSlice?) -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 152.dp
) {
    val emiColors = LocalEmiColors.current
    val dividerColor = MaterialTheme.colorScheme.surface
    val selectionRingColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val textMeasurer = rememberTextMeasurer()

    val safeTotal = (principalRatio + interestRatio).coerceAtLeast(0.0001f)
    val normPrincipal = (principalRatio / safeTotal).coerceIn(0f, 1f)
    val normInterest = (interestRatio / safeTotal).coerceIn(0f, 1f)

    val animatedPrincipal by animateFloatAsState(
        targetValue = normPrincipal,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "piePrincipalSweep"
    )
    val animatedInterest by animateFloatAsState(
        targetValue = normInterest,
        animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
        label = "pieInterestSweep"
    )

    val principalPctInt = (normPrincipal * 100f).roundToInt()
    val interestPctInt = (100 - principalPctInt).coerceAtLeast(0)
    val chartDesc = "Loan breakdown pie chart: Principal $principalPctInt percent, Interest $interestPctInt percent"

    val labelStyle = MaterialTheme.typography.labelMedium.merge(TabularNumberStyle).copy(
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp
    )

    Box(
        modifier = modifier
            .size(size)
            .semantics { contentDescription = chartDesc },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(normPrincipal, selectedSlice) {
                    detectTapGestures { offset ->
                        val dx = offset.x - this.size.width / 2f
                        val dy = offset.y - this.size.height / 2f
                        val radius = minOf(this.size.width, this.size.height) / 2f
                        if (dx * dx + dy * dy <= radius * radius) {
                            val degFrom3 = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                            val clockwiseFrom12 = (degFrom3 + 90f + 360f) % 360f
                            val tapped = if (clockwiseFrom12 <= normPrincipal * 360f) {
                                PieSlice.PRINCIPAL
                            } else {
                                PieSlice.INTEREST
                            }
                            onSelectSlice(if (selectedSlice == tapped) null else tapped)
                        }
                    }
                }
        ) {
            val popOffsetPx = 5.dp.toPx()
            val inset = popOffsetPx
            val diameter = minOf(this.size.width, this.size.height) - inset * 2f
            if (diameter <= 0f) return@Canvas

            val baseTopLeft = Offset(
                (this.size.width - diameter) / 2f,
                (this.size.height - diameter) / 2f
            )
            val pieSize = Size(diameter, diameter)
            val radius = diameter / 2f
            val center = Offset(this.size.width / 2f, this.size.height / 2f)

            val principalSweep = animatedPrincipal * 360f
            val interestSweep = animatedInterest * 360f

            fun sliceOffset(startAngleDeg: Float, sweepDeg: Float, isSelected: Boolean): Offset {
                if (!isSelected || sweepDeg >= 359f) return Offset.Zero
                val midRad = Math.toRadians((startAngleDeg + sweepDeg / 2f).toDouble())
                return Offset(
                    (cos(midRad) * popOffsetPx).toFloat(),
                    (sin(midRad) * popOffsetPx).toFloat()
                )
            }

            // Principal slice
            if (principalSweep > 0.2f) {
                val isSelected = selectedSlice == PieSlice.PRINCIPAL
                val alpha = if (selectedSlice == null || isSelected) 1f else 0.72f
                val sliceShift = sliceOffset(-90f, principalSweep, isSelected)
                val sliceColor = emiColors.principalColor.copy(alpha = alpha)

                drawArc(
                    color = sliceColor,
                    startAngle = -90f,
                    sweepAngle = principalSweep,
                    useCenter = true,
                    topLeft = baseTopLeft + sliceShift,
                    size = pieSize
                )
                if (interestSweep > 0.5f) {
                    drawArc(
                        color = dividerColor,
                        startAngle = -90f,
                        sweepAngle = principalSweep,
                        useCenter = true,
                        topLeft = baseTopLeft + sliceShift,
                        size = pieSize,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                if (isSelected) {
                    drawArc(
                        color = selectionRingColor,
                        startAngle = -90f,
                        sweepAngle = principalSweep,
                        useCenter = false,
                        topLeft = baseTopLeft + sliceShift,
                        size = pieSize,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Interest slice
            if (interestSweep > 0.2f) {
                val startDeg = -90f + principalSweep
                val isSelected = selectedSlice == PieSlice.INTEREST
                val alpha = if (selectedSlice == null || isSelected) 1f else 0.72f
                val sliceShift = sliceOffset(startDeg, interestSweep, isSelected)
                val sliceColor = emiColors.interestColor.copy(alpha = alpha)

                drawArc(
                    color = sliceColor,
                    startAngle = startDeg,
                    sweepAngle = interestSweep,
                    useCenter = true,
                    topLeft = baseTopLeft + sliceShift,
                    size = pieSize
                )
                if (principalSweep > 0.5f) {
                    drawArc(
                        color = dividerColor,
                        startAngle = startDeg,
                        sweepAngle = interestSweep,
                        useCenter = true,
                        topLeft = baseTopLeft + sliceShift,
                        size = pieSize,
                        style = Stroke(width = 2.5.dp.toPx())
                    )
                }
                if (isSelected) {
                    drawArc(
                        color = selectionRingColor,
                        startAngle = startDeg,
                        sweepAngle = interestSweep,
                        useCenter = false,
                        topLeft = baseTopLeft + sliceShift,
                        size = pieSize,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }

            // Draw slice percentage labels inside sufficiently wide wedges
            if (principalSweep >= 46f) {
                val sliceShift = sliceOffset(-90f, principalSweep, selectedSlice == PieSlice.PRINCIPAL)
                val midRad = Math.toRadians((-90f + principalSweep / 2f).toDouble())
                val textColor = accessibleSliceTextColor(
                    background = emiColors.principalColor,
                    darkCandidate = Color(0xFF06281E)
                )
                val measured = textMeasurer.measure(
                    text = "$principalPctInt%",
                    style = labelStyle.copy(color = textColor)
                )
                val labelPos = center + sliceShift + Offset(
                    (radius * 0.56f * cos(midRad)).toFloat() - measured.size.width / 2f,
                    (radius * 0.56f * sin(midRad)).toFloat() - measured.size.height / 2f
                )
                drawText(measured, topLeft = labelPos)
            }

            if (interestSweep >= 46f) {
                val startDeg = -90f + principalSweep
                val sliceShift = sliceOffset(startDeg, interestSweep, selectedSlice == PieSlice.INTEREST)
                val midRad = Math.toRadians((startDeg + interestSweep / 2f).toDouble())
                val textColor = accessibleSliceTextColor(
                    background = emiColors.interestColor,
                    darkCandidate = Color(0xFF331B00)
                )
                val measured = textMeasurer.measure(
                    text = "$interestPctInt%",
                    style = labelStyle.copy(color = textColor)
                )
                val labelPos = center + sliceShift + Offset(
                    (radius * 0.56f * cos(midRad)).toFloat() - measured.size.width / 2f,
                    (radius * 0.56f * sin(midRad)).toFloat() - measured.size.height / 2f
                )
                drawText(measured, topLeft = labelPos)
            }
        }
    }
}

private fun accessibleSliceTextColor(background: Color, darkCandidate: Color): Color {
    val bgLum = background.luminance()
    val darkContrast = contrastRatio(bgLum, darkCandidate.luminance())
    val lightContrast = contrastRatio(bgLum, Color.White.luminance())
    return if (darkContrast >= lightContrast) darkCandidate else Color.White
}

private fun contrastRatio(lumA: Float, lumB: Float): Float {
    val lighter = max(lumA, lumB)
    val darker = min(lumA, lumB)
    return (lighter + 0.05f) / (darker + 0.05f)
}

