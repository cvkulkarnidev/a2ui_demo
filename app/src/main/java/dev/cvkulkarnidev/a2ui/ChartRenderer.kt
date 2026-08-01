package dev.cvkulkarnidev.a2ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.*
import kotlin.math.max

private data class ChartPoint(
    val label: String,
    val value: Double
)

@Composable
fun InteractiveChart(
    component: JsonObject,
    data: JsonElement,
    scopeData: JsonElement
) {
    val points = remember(component, data, scopeData) {
        chartPoints(component, data, scopeData)
    }
    val modes = remember(component, points) {
        chartModes(component, points)
    }
    var selectedModeIndex by remember(component["id"]?.jsonPrimitive?.content, modes) {
        mutableIntStateOf(0)
    }
    var showConversionOptions by remember(component["id"]?.jsonPrimitive?.content, modes) {
        mutableStateOf(false)
    }
    val safeMode = modes.getOrElse(selectedModeIndex.coerceIn(0, modes.lastIndex.coerceAtLeast(0))) {
        "table"
    }
    val palette = chartPalette()

    if (showConversionOptions) {
        AlertDialog(
            onDismissRequest = { showConversionOptions = false },
            title = { Text("Convert chart") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXs)) {
                    modes.forEachIndexed { index, mode ->
                        TextButton(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                selectedModeIndex = index
                                showConversionOptions = false
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(mode.chartModeLabel())
                                if (mode == safeMode) {
                                    Text(
                                        "Current",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConversionOptions = false }) {
                    Text("Close")
                }
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(OneUiTokens.radiusLarge),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(OneUiTokens.spaceLg),
            verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceMd)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = dynamicString(component["title"], data, scopeData).ifBlank { "Chart" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (modes.size > 1) "Long press chart to convert view" else safeMode.chartModeLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                AssistChip(
                    enabled = modes.size > 1,
                    onClick = {
                        if (modes.size > 1) showConversionOptions = true
                    },
                    label = { Text("Convert") }
                )
            }

            if (points.isEmpty()) {
                Text(
                    "No chart data",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .pointerInput(modes) {
                            detectTapGestures(
                                onLongPress = {
                                    if (modes.size > 1) {
                                        showConversionOptions = true
                                    }
                                }
                            )
                        }
                ) {
                    when (safeMode) {
                        "pie" -> PieChart(points, palette)
                        "line" -> LineChart(points, palette.first())
                        "table" -> ChartTable(points)
                        else -> BarChart(points, palette.first())
                    }
                }

                if (safeMode != "table") {
                    ChartLegend(points, palette)
                }
            }
        }
    }
}

@Composable
private fun BarChart(points: List<ChartPoint>, color: Color) {
    val maxValue = points.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val gap = 10.dp.toPx()
        val baseline = size.height - 10.dp.toPx()
        val barWidth = ((size.width - gap * (points.size + 1)) / points.size).coerceAtLeast(8.dp.toPx())

        points.forEachIndexed { index, point ->
            val left = gap + index * (barWidth + gap)
            val barHeight = ((point.value / maxValue) * (size.height - 28.dp.toPx())).toFloat()
            drawRoundRect(
                color = color.copy(alpha = 0.82f),
                topLeft = Offset(left, baseline - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

@Composable
private fun LineChart(points: List<ChartPoint>, color: Color) {
    val maxValue = points.maxOfOrNull { it.value }?.takeIf { it > 0.0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        if (points.size == 1) {
            drawCircle(color = color, radius = 7.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f))
            return@Canvas
        }

        val leftPad = 12.dp.toPx()
        val rightPad = 12.dp.toPx()
        val topPad = 12.dp.toPx()
        val bottomPad = 18.dp.toPx()
        val usableWidth = size.width - leftPad - rightPad
        val usableHeight = size.height - topPad - bottomPad

        val offsets = points.mapIndexed { index, point ->
            val x = leftPad + (index.toFloat() / (points.lastIndex).toFloat()) * usableWidth
            val y = topPad + usableHeight - ((point.value / maxValue) * usableHeight).toFloat()
            Offset(x, y)
        }

        offsets.zipWithNext().forEach { (start, end) ->
            drawLine(color = color, start = start, end = end, strokeWidth = 4.dp.toPx())
        }
        offsets.forEach { drawCircle(color = color, radius = 5.dp.toPx(), center = it) }
    }
}

@Composable
private fun PieChart(points: List<ChartPoint>, palette: List<Color>) {
    val total = points.sumOf { max(0.0, it.value) }.takeIf { it > 0.0 } ?: 1.0
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val diameter = size.minDimension * 0.82f
        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
        var startAngle = -90f

        points.forEachIndexed { index, point ->
            val sweep = ((max(0.0, point.value) / total) * 360.0).toFloat()
            drawArc(
                color = palette[index % palette.size],
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter)
            )
            startAngle += sweep
        }

        drawCircle(
            color = Color.White.copy(alpha = 0.92f),
            radius = diameter * 0.22f,
            center = Offset(size.width / 2f, size.height / 2f)
        )
    }
}

@Composable
private fun ChartTable(points: List<ChartPoint>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXs)
    ) {
        ChartTableRow("Label", "Value", header = true)
        points.forEach { point ->
            ChartTableRow(point.label, formatNumber(point.value), header = false)
        }
    }
}

@Composable
private fun ChartTableRow(label: String, value: String, header: Boolean) {
    Surface(
        modifier = Modifier.widthIn(min = 280.dp),
        shape = RoundedCornerShape(OneUiTokens.radiusSmall),
        color = if (header) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
        } else {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
        }
    ) {
        Row(
            modifier = Modifier.padding(
                horizontal = OneUiTokens.spaceMd,
                vertical = OneUiTokens.spaceSm
            ),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                label,
                modifier = Modifier.weight(1f),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header) FontWeight.Bold else null
            )
            Text(
                value,
                modifier = Modifier.weight(1f),
                style = if (header) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
                fontWeight = if (header) FontWeight.Bold else null
            )
        }
    }
}

@Composable
private fun ChartLegend(points: List<ChartPoint>, palette: List<Color>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(OneUiTokens.spaceSm),
        verticalArrangement = Arrangement.spacedBy(OneUiTokens.spaceXs)
    ) {
        points.take(8).forEachIndexed { index, point ->
            AssistChip(
                onClick = {},
                leadingIcon = {
                    Canvas(Modifier.size(10.dp)) {
                        drawCircle(color = palette[index % palette.size])
                    }
                },
                label = {
                    Text(
                        "${point.label}: ${formatNumber(point.value)}",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

@Composable
private fun chartPalette(): List<Color> = listOf(
    MaterialTheme.colorScheme.primary,
    MaterialTheme.colorScheme.secondary,
    MaterialTheme.colorScheme.tertiary,
    MaterialTheme.colorScheme.error,
    MaterialTheme.colorScheme.primary.copy(alpha = 0.58f),
    MaterialTheme.colorScheme.secondary.copy(alpha = 0.58f),
    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.58f)
)

private fun chartPoints(
    component: JsonObject,
    data: JsonElement,
    scopeData: JsonElement
): List<ChartPoint> {
    val raw = resolveJson(component["data"], data, scopeData)
    val labelKey = component["labelKey"]?.jsonPrimitive?.contentOrNull
    val valueKey = component["valueKey"]?.jsonPrimitive?.contentOrNull

    return (raw as? JsonArray).orEmpty().mapIndexedNotNull { index, item ->
        when (item) {
            is JsonObject -> {
                val label = labelFrom(item, labelKey).ifBlank { "Item ${index + 1}" }
                val value = numberFrom(item, valueKey) ?: return@mapIndexedNotNull null
                ChartPoint(label, value)
            }
            is JsonPrimitive -> item.doubleOrNull?.let { ChartPoint("Item ${index + 1}", it) }
            else -> null
        }
    }
}

private fun chartModes(component: JsonObject, points: List<ChartPoint>): List<String> {
    val requested = (component["modes"] as? JsonArray)
        ?.mapNotNull { it.jsonPrimitive.contentOrNull?.normalizeMode() }
        ?.filter { it in setOf("pie", "bar", "line", "table") }
        .orEmpty()

    val first = component["chartType"]?.jsonPrimitive?.contentOrNull?.normalizeMode()?.takeIfValidMode()
        ?: component["display"]?.jsonPrimitive?.contentOrNull?.normalizeMode()
            ?.takeIfValidMode()
        ?: "bar"

    val defaults = if (points.size <= 1) {
        listOf(first, "table")
    } else {
        listOf(first, "bar", "line", "pie", "table")
    }

    return (requested.ifEmpty { defaults }).distinct()
}

private fun String.normalizeMode(): String = when (lowercase()) {
    "histogram", "column" -> "bar"
    "donut", "doughnut" -> "pie"
    else -> lowercase()
}

private fun String.takeIfValidMode(): String? =
    takeIf { it in setOf("pie", "bar", "line", "table") }

private fun String.chartModeLabel(): String = when (this) {
    "pie" -> "Pie chart"
    "bar" -> "Bar / histogram"
    "line" -> "Line chart"
    "table" -> "Table"
    else -> titlecase()
}

private fun resolveJson(value: JsonElement?, data: JsonElement, scopeData: JsonElement): JsonElement? = when (value) {
    is JsonObject -> value["path"]?.jsonPrimitive?.contentOrNull
        ?.let { chartLookup(data, it, scopeData) }
        ?: value
    else -> value
}

private fun dynamicString(value: JsonElement?, data: JsonElement, scopeData: JsonElement): String = when (value) {
    null, JsonNull -> ""
    is JsonPrimitive -> value.content
    is JsonObject -> value["path"]?.jsonPrimitive?.contentOrNull
        ?.let { chartLookup(data, it, scopeData) }
        ?.jsonPrimitive
        ?.contentOrNull ?: ""
    else -> value.toString()
}

private fun labelFrom(item: JsonObject, explicitKey: String?): String {
    val keys = listOfNotNull(explicitKey) + listOf("label", "name", "category", "segment", "month", "period", "x")
    return keys.firstNotNullOfOrNull { key -> item[key]?.jsonPrimitive?.contentOrNull }.orEmpty()
}

private fun numberFrom(item: JsonObject, explicitKey: String?): Double? {
    val keys = listOfNotNull(explicitKey) + listOf("value", "count", "amount", "revenue", "share", "percent", "y")
    return keys.firstNotNullOfOrNull { key -> item[key]?.jsonPrimitive?.doubleOrNull }
}

private fun chartLookup(root: JsonElement, path: String, scopeData: JsonElement = root): JsonElement? {
    if (path.isBlank() || path == "/") return root
    val base = if (path.startsWith("/")) root else scopeData
    return path.trim('/').split('/').filter { it.isNotEmpty() }
        .map { it.replace("~1", "/").replace("~0", "~") }
        .fold(base as JsonElement?) { current, key ->
            when (current) {
                is JsonObject -> current[key]
                is JsonArray -> key.toIntOrNull()?.let(current::getOrNull)
                else -> null
            }
        }
}

private fun formatNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)

private fun String.titlecase(): String =
    replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
