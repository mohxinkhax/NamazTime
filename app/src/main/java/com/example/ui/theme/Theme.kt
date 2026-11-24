package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.model.DarkThemeColorPreset

@Composable
fun NamazTimeTheme(
    preset: DarkThemeColorPreset = DarkThemeColorPreset.OLED_GOLD,
    activeAccentColor: Color = Color(preset.accentHex),
    content: @Composable () -> Unit
) {
    val backgroundColor = Color(preset.backgroundHex)
    val surfaceColor = Color(preset.surfaceHex)
    val surfaceVariantColor = Color(preset.surfaceVariantHex)

    val colorScheme = darkColorScheme(
        primary = activeAccentColor,
        onPrimary = OledBlack,
        secondary = Color(preset.secondaryHex),
        onSecondary = OledBlack,
        background = backgroundColor,
        onBackground = SoftWhiteText,
        surface = surfaceColor,
        onSurface = SoftWhiteText,
        surfaceVariant = surfaceVariantColor,
        onSurfaceVariant = MutedSlateText,
        outline = SubtleBorder,
        outlineVariant = Color(0xFF162038)
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
