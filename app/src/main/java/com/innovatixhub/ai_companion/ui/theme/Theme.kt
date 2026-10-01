package com.eva.ai.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EvaPink,
    onPrimary = Color.White,
    primaryContainer = EvaPurple.copy(alpha = 0.24f),
    onPrimaryContainer = Color.White,
    secondary = EvaPurple,
    onSecondary = Color.White,
    tertiary = EvaCoral,
    onTertiary = Color.White,
    error = EvaDanger,
    onError = Color.White,
    background = EvaInk,
    onBackground = Color.White,
    surface = EvaInkRaised,
    onSurface = Color.White,
    surfaceVariant = EvaInkHigh,
    onSurfaceVariant = Color.White.copy(alpha = 0.72f),
    outline = Color.White.copy(alpha = 0.14f),
    outlineVariant = Color.White.copy(alpha = 0.08f),
    inverseSurface = Color(0xFFF7EFF8),
    inverseOnSurface = EvaOnDark
)

private val LightColorScheme = lightColorScheme(
    primary = EvaPink,
    onPrimary = Color.White,
    primaryContainer = EvaPink.copy(alpha = 0.14f),
    onPrimaryContainer = EvaOnDark,
    secondary = EvaPurple,
    onSecondary = Color.White,
    tertiary = EvaCoral,
    onTertiary = Color.White,
    error = EvaDanger,
    onError = Color.White,
    background = EvaMist,
    onBackground = EvaOnDark,
    surface = EvaMistRaised,
    onSurface = EvaOnDark,
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = EvaMutedOnDark,
    outline = Color.Black.copy(alpha = 0.12f),
    outlineVariant = Color.Black.copy(alpha = 0.08f),
    inverseSurface = EvaOnDark,
    inverseOnSurface = Color.White
)

private val EvaShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AICompanionTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = EvaShapes,
        content = content
    )
}
