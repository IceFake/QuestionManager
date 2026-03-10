package com.example.questionmanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Theme Colors
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

// Dark Theme Colors
val Purple40 = Color(0xFF6650A4)
val PurpleGrey40 = Color(0xFF625B71)
val Pink40 = Color(0xFF7D5260)

// Custom App Colors — Light
val StatusPendingLight = Color(0xFFFFA726)     // 橙色 - 等待生成
val StatusGeneratingLight = Color(0xFF42A5F5)  // 蓝色 - 生成中
val StatusCompletedLight = Color(0xFF66BB6A)   // 绿色 - 已完成
val StatusErrorLight = Color(0xFFEF5350)       // 红色 - 错误

// Custom App Colors — Dark (brighter for visibility on dark backgrounds)
val StatusPendingDark = Color(0xFFFFB74D)
val StatusGeneratingDark = Color(0xFF64B5F6)
val StatusCompletedDark = Color(0xFF81C784)
val StatusErrorDark = Color(0xFFE57373)

// Adaptive accessors
val StatusPending: Color
    @Composable get() = if (isSystemInDarkTheme()) StatusPendingDark else StatusPendingLight
val StatusGenerating: Color
    @Composable get() = if (isSystemInDarkTheme()) StatusGeneratingDark else StatusGeneratingLight
val StatusCompleted: Color
    @Composable get() = if (isSystemInDarkTheme()) StatusCompletedDark else StatusCompletedLight
val StatusError: Color
    @Composable get() = if (isSystemInDarkTheme()) StatusErrorDark else StatusErrorLight

