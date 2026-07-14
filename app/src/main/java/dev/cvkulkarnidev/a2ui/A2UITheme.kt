package dev.cvkulkarnidev.a2ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5BD6),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE5E4FF),
    onPrimaryContainer = Color(0xFF17164A),
    secondary = Color(0xFF00A67E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB9F4DF),
    onSecondaryContainer = Color(0xFF00382A),
    tertiary = Color(0xFFFF8A3D),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDBC7),
    background = Color(0xFFF7F7FC),
    onBackground = Color(0xFF1A1B25),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1B25),
    surfaceVariant = Color(0xFFEEEFF7),
    onSurfaceVariant = Color(0xFF5C5D6B),
    outline = Color(0xFFC7C8D4),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFC5C3FF),
    onPrimary = Color(0xFF2B296B),
    primaryContainer = Color(0xFF42408E),
    onPrimaryContainer = Color(0xFFE5E4FF),
    secondary = Color(0xFF77DCBD),
    onSecondary = Color(0xFF00382A),
    secondaryContainer = Color(0xFF00513E),
    onSecondaryContainer = Color(0xFFB9F4DF),
    tertiary = Color(0xFFFFB68A),
    onTertiary = Color(0xFF542100),
    tertiaryContainer = Color(0xFF773200),
    background = Color(0xFF111218),
    onBackground = Color(0xFFE4E1E9),
    surface = Color(0xFF191A21),
    onSurface = Color(0xFFE4E1E9),
    surfaceVariant = Color(0xFF45464F),
    onSurfaceVariant = Color(0xFFC7C5D0)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, lineHeight = 38.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp, lineHeight = 32.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 21.sp, lineHeight = 27.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 17.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 15.sp),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
    bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
)

@Composable
fun A2UITheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
