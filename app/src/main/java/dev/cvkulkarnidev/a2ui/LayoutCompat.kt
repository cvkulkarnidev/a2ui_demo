package dev.cvkulkarnidev.a2ui

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.unit.dp

/**
 * Gives catalog-rendered compact components a predictable width even though
 * they are rendered outside a compile-time RowScope.
 */
fun Modifier.weight(weight: Float, fill: Boolean = true): Modifier {
    val minWidth = if (fill) 88.dp else 72.dp
    val maxWidth = if (weight >= 1f) 116.dp else 96.dp
    return widthIn(min = minWidth, max = maxWidth)
}
