package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.animation.AnimatedVisibility
import com.example.data.LayerFarmLog
import java.util.Locale

@Composable
fun LargeNumericStepper(
    title: String,
    value: String,
    unit: String,
    onIncrement: (Float) -> Unit,
    onDecrement: (Float) -> Unit,
    steps: List<Float>,
    onValueChange: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        fontSize = 24.sp
                    ),
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = if (isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ),
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("${title.lowercase().replace(" ", "_")}_input"),
                    suffix = {
                        Text(
                            text = unit,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            if (isError && !errorMessage.isNullOrEmpty()) {
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Massive high-contrast action buttons for dusty finger taps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Decrement group
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    steps.reversed().forEach { step ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.errorContainer)
                                .clickable { onDecrement(step) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "-${formatStep(step)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Increment group
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    steps.forEach { step ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { onIncrement(step) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+${formatStep(step)}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatStep(step: Float): String {
    return if (step % 1.0f == 0.0f) {
        step.toInt().toString()
    } else {
        step.toString()
    }
}

data class DailyTrendData(
    val date: String,
    val eggCount: Int,
    val eggWeight: Float,
    val feedAmount: Float,
    val fcr: Float
)

@Composable
fun FarmTrendChartsSection(
    logs: List<LayerFarmLog>,
    modifier: Modifier = Modifier
) {
    val sortedDailyData = remember(logs) {
        logs.groupBy { it.date }
            .map { (date, dailyLogs) ->
                val totalEggs = dailyLogs.sumOf { it.eggCount }
                val totalWeight = dailyLogs.sumOf { it.eggWeight.toDouble() }.toFloat()
                val totalFeed = dailyLogs.sumOf { it.feedAmount.toDouble() }.toFloat()
                val fcr = if (totalWeight > 0) totalFeed / totalWeight else 0f
                DailyTrendData(
                    date = date,
                    eggCount = totalEggs,
                    eggWeight = totalWeight,
                    feedAmount = totalFeed,
                    fcr = fcr
                )
            }
            .sortedBy { it.date }
    }

    if (sortedDailyData.isEmpty()) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = "No trend data",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Data Tren Belum Cukup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Silakan tambahkan catatan harian terlebih dahulu untuk melihat grafik tren produksi dan FCR secara real-time.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InteractiveLineChart(
                data = sortedDailyData,
                valueSelector = { it.eggWeight },
                valueFormatter = { String.format(Locale.US, "%.1f kg", it) },
                title = "Tren Produksi Telur (Kg)",
                unit = "kg",
                lineColor = MaterialTheme.colorScheme.primary
            )

            InteractiveLineChart(
                data = sortedDailyData,
                valueSelector = { it.fcr },
                valueFormatter = { if (it > 0f) String.format(Locale.US, "%.2f", it) else "-" },
                title = "Tren Feed Conversion Ratio (FCR)",
                unit = "",
                lineColor = Color(0xFF2E7D32), // Custom green for FCR
                isFcr = true
            )
        }
    }
}

@Composable
fun InteractiveLineChart(
    data: List<DailyTrendData>,
    valueSelector: (DailyTrendData) -> Float,
    valueFormatter: (Float) -> String,
    title: String,
    unit: String,
    lineColor: Color,
    isFcr: Boolean = false,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(data) { mutableStateOf<Int?>(null) }
    val density = LocalDensity.current

    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface

    val labelPaint = remember(density, onSurfaceVariantColor) {
        android.graphics.Paint().apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.RIGHT
            isAntiAlias = true
        }
    }

    val xLabelPaint = remember(density, onSurfaceVariantColor) {
        android.graphics.Paint().apply {
            color = onSurfaceVariantColor.toArgb()
            textSize = with(density) { 10.sp.toPx() }
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Ketuk grafik untuk detail nilai per tanggal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Real-time Trend Indicator (comparing last 2 days)
                if (data.size >= 2) {
                    val lastVal = valueSelector(data.last())
                    val prevVal = valueSelector(data[data.size - 2])
                    val diff = lastVal - prevVal
                    val isBetter = if (isFcr) diff < 0 else diff > 0 // Lower FCR is better, higher production is better
                    val isZero = diff == 0f

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isZero -> MaterialTheme.colorScheme.surfaceVariant
                                    isBetter -> Color(0xFFE8F5E9)
                                    else -> Color(0xFFFCE4EC)
                                }
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (isBetter) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                            contentDescription = "Trend Icon",
                            tint = when {
                                isZero -> MaterialTheme.colorScheme.onSurfaceVariant
                                isBetter -> Color(0xFF2E7D32)
                                else -> Color(0xFFC2185B)
                            },
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when {
                                isZero -> "Stabil"
                                isBetter -> "Membaik"
                                else -> "Tinjau"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                isZero -> MaterialTheme.colorScheme.onSurfaceVariant
                                isBetter -> Color(0xFF1B5E20)
                                else -> Color(0xFF880E4F)
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Graph Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(data) {
                            detectTapGestures { offset ->
                                val width = size.width
                                val paddingLeft = 100f
                                val paddingRight = 40f
                                val chartWidth = width - paddingLeft - paddingRight

                                if (data.size > 1 && offset.x >= paddingLeft && offset.x <= width - paddingRight) {
                                    val ratio = (offset.x - paddingLeft) / chartWidth
                                    val index = (ratio * (data.size - 1)).coerceIn(0f, (data.size - 1).toFloat()).plus(0.5f).toInt()
                                    selectedIndex = if (index in data.indices) index else null
                                }
                            }
                        }
                ) {
                    val width = size.width
                    val height = size.height

                    val paddingLeft = 100f
                    val paddingRight = 40f
                    val paddingTop = 40f
                    val paddingBottom = 80f

                    val chartWidth = width - paddingLeft - paddingRight
                    val chartHeight = height - paddingTop - paddingBottom

                    // Calculate Min and Max
                    val rawMax = data.maxOfOrNull { valueSelector(it) } ?: 0f
                    val rawMin = data.minOfOrNull { valueSelector(it) } ?: 0f
                    val range = (rawMax - rawMin).coerceAtLeast(0.1f)
                    
                    val yMin = (rawMin - range * 0.15f).coerceAtLeast(0f)
                    val yMax = rawMax + range * 0.15f
                    val yRange = yMax - yMin

                    // Draw Horizontal Gridlines & Y-axis Labels
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val gridY = paddingTop + chartHeight - (i.toFloat() / gridLines) * chartHeight
                        val gridValue = yMin + (i.toFloat() / gridLines) * yRange

                        drawLine(
                            color = onSurfaceVariantColor.copy(alpha = 0.15f),
                            start = Offset(paddingLeft, gridY),
                            end = Offset(width - paddingRight, gridY),
                            strokeWidth = 1f
                        )

                        drawContext.canvas.nativeCanvas.drawText(
                            if (isFcr) String.format(Locale.US, "%.2f", gridValue) else String.format(Locale.US, "%.1f", gridValue),
                            paddingLeft - 15f,
                            gridY + 10f,
                            labelPaint
                        )
                    }

                    fun getX(index: Int): Float {
                        if (data.size <= 1) return paddingLeft + chartWidth / 2f
                        return paddingLeft + (index.toFloat() / (data.size - 1)) * chartWidth
                    }

                    fun getY(value: Float): Float {
                        return paddingTop + chartHeight - ((value - yMin) / yRange) * chartHeight
                    }

                    // Draw Line Path
                    if (data.isNotEmpty()) {
                        val path = Path().apply {
                            moveTo(getX(0), getY(valueSelector(data[0])))
                            for (i in 1 until data.size) {
                                lineTo(getX(i), getY(valueSelector(data[i])))
                            }
                        }

                        // Draw Area Fill under the line
                        val fillPath = Path().apply {
                            addPath(path)
                            if (data.size > 1) {
                                lineTo(getX(data.size - 1), paddingTop + chartHeight)
                                lineTo(getX(0), paddingTop + chartHeight)
                            } else {
                                lineTo(getX(0), paddingTop + chartHeight)
                            }
                            close()
                        }

                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent),
                                startY = paddingTop,
                                endY = paddingTop + chartHeight
                            )
                        )

                        // Draw beautiful stroke
                        drawPath(
                            path = path,
                            color = lineColor,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        )

                        // Draw X-axis Dates Labels
                        val step = (data.size / 5).coerceAtLeast(1)
                        for (i in data.indices) {
                            if (i % step == 0 || i == data.size - 1) {
                                val labelX = getX(i)
                                val labelY = paddingTop + chartHeight + 40f
                                drawContext.canvas.nativeCanvas.drawText(
                                    formatShortDate(data[i].date),
                                    labelX,
                                    labelY,
                                    xLabelPaint
                                )
                            }
                        }

                        // Draw highlighted point line & circle if selected
                        selectedIndex?.let { index ->
                            if (index in data.indices) {
                                val hPoint = data[index]
                                val hValue = valueSelector(hPoint)
                                val hX = getX(index)
                                val hY = getY(hValue)

                                drawLine(
                                    color = lineColor.copy(alpha = 0.5f),
                                    start = Offset(hX, paddingTop),
                                    end = Offset(hX, paddingTop + chartHeight),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )

                                drawCircle(
                                    color = lineColor.copy(alpha = 0.25f),
                                    radius = 12.dp.toPx(),
                                    center = Offset(hX, hY)
                                )

                                drawCircle(
                                    color = lineColor,
                                    radius = 6.dp.toPx(),
                                    center = Offset(hX, hY)
                                )

                                drawCircle(
                                    color = Color.White,
                                    radius = 2.5.dp.toPx(),
                                    center = Offset(hX, hY)
                                )
                            }
                        }
                    }
                }
            }

            // Interactive Tooltip / Detail Panel beneath the chart
            AnimatedVisibility(visible = selectedIndex != null) {
                selectedIndex?.let { index ->
                    if (index in data.indices) {
                        val point = data[index]
                        val value = valueSelector(point)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Detail Tanggal: ${formatFullDate(point.date)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = title.substringBefore(" ("),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Black,
                                    color = lineColor
                                )
                            }
                            Text(
                                text = valueFormatter(value),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Quick legend / statistics summary row
            if (data.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val average = data.map { valueSelector(it) }.average().toFloat()
                    val maxVal = data.maxOf { valueSelector(it) }
                    val minVal = data.minOf { valueSelector(it) }

                    Text(
                        text = "Rata-rata: ${valueFormatter(average)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Tertinggi: ${valueFormatter(maxVal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Terendah: ${valueFormatter(minVal)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatShortDate(dateStr: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = java.text.SimpleDateFormat("dd MMM", Locale("in", "ID"))
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        if (dateStr.length >= 5) dateStr.substring(5) else dateStr
    }
}

private fun formatFullDate(dateStr: String): String {
    return try {
        val parser = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = java.text.SimpleDateFormat("dd MMMM yyyy", Locale("in", "ID"))
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
