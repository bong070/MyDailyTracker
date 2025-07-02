package com.bbks.mydailytracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 성공: 녹색, 실패: 회색 (라이트/다크 공통 톤)
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8BC34A), // 초록색 버튼
    background = Color(0xFFF7EBD5), // 전체 배경
    surface = Color(0xFFFDF5E6),   // 카드 내부 배경
    onSurface = Color(0xFF212121), // 텍스트 색
    onSurfaceVariant = Color(0xFF9E9E9E), // 아이콘 회색
    secondary = Color(0xFFFFF8E1), // 입력창 배경색
    outline = Color(0xFFBDBDBD),   // 테두리/아웃라인 색
    error = Color.Red // 다이얼로그용
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),          // 밝은 그린 (진행률 바 등)
    onPrimary = Color.Black,
    background = Color(0xFF2E261E),           // 어두운 베이지 배경
    onBackground = Color(0xFFEFEFEF),
    surface = Color(0xFF3A2F24),             // 카드 배경
    onSurface = Color(0xFFEADBB6),            // 밝은 텍스트
    surfaceVariant = Color(0xFF5B4F42),   // 진행률 트랙 색
    onSurfaceVariant = Color(0xFF9E9E9E), // 아이콘 등 부가 텍스트 회색
    secondary = Color(0xFF4A4036),            // 입력창 배경 느낌
    outline = Color(0xFF8D8374),              // Divider/테두리 느낌
    outlineVariant = Color(0xFFEADBB6),    // 습관 카드 간 구분선
    error = Color(0xFFCF6679)
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
