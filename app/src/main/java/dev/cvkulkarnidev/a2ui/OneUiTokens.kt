package dev.cvkulkarnidev.a2ui

import androidx.compose.ui.unit.dp

/**
 * Renderer-owned One UI-inspired tokens.
 *
 * These are intentionally centralized so component renderers do not rely on
 * scenario-specific dimensions or duplicated magic numbers.
 */
object OneUiTokens {
    val spaceXs = 4.dp
    val spaceSm = 8.dp
    val spaceMd = 12.dp
    val spaceLg = 16.dp
    val spaceXl = 20.dp
    val spaceXxl = 24.dp
    val sectionSpace = 28.dp

    val radiusSmall = 12.dp
    val radiusMedium = 16.dp
    val radiusLarge = 22.dp
    val radiusSurface = 28.dp

    val screenHorizontalPadding = 20.dp
    val screenBottomPadding = 32.dp
    val minTouchTarget = 48.dp

    val metricMinWidth = 92.dp
    val metricMaxWidth = 148.dp
    val compactChipMaxWidth = 180.dp
    val heroMinHeight = 156.dp
    val heroMaxHeight = 260.dp
}
