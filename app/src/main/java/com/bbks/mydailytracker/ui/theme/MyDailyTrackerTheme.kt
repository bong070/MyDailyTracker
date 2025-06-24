package com.bbks.mydailytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 성공: 녹색, 실패: 회색 (라이트/다크 공통 톤)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4CAF50),    // Success Green
    error = Color(0xFF9E9E9E),      // Failure Gray
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),    // Success Green (lighter tone)
    error = Color(0xFFBDBDBD),      // Failure Gray (lighter tone)
    background = Color(0xFF121212),
    onBackground = Color(0xFFFFFFFF),
    surface = Color(0xFF1E1E1E),
    onSurface = Color(0xFFFFFFFF)
)

@Composable
fun MyDailyTrackerTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (useDarkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
