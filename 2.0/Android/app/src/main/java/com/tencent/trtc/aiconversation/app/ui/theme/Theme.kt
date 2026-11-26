// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - Material3 Theme (light)

package com.tencent.trtc.aiconversation.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- 全局配色（取自设计稿「AI 对话Demo-亮色版本」） ---
val BackgroundColor = Color(0xFFF5F7FB)
val CardColor = Color(0xFFFFFFFF)
val LineColor = Color(0xFFDFE5EF)

val Ink = Color(0xFF182230)
val Muted = Color(0xFF667085)

val Brand = Color(0xFF236CA8)
val BrandInk = Color(0xFF123F63)
val BrandSoft = Color(0xFFB1E2FF)
val BrandSoftLine = Color(0xFF8BCBF3)

val Danger = Color(0xFFF17683)
val OffBg = Color(0xFFE9EEF5)
val OffLine = Color(0xFFCBD5E1)

val Bubble = Color(0xFFE2F1FF)
val BubbleLine = Color(0xFFC4DEF5)
val BubbleInk = Color(0xFF24445F)

private val LightColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    background = BackgroundColor,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
)

@Composable
fun AIConversationTheme(
    content: @Composable () -> Unit
) {
    // 当前 Demo 统一使用亮色调，不跟随系统暗色；系统栏样式由 Activity 的 enableEdgeToEdge 设置。
    MaterialTheme(
        colorScheme = LightColorScheme,
        content = content
    )
}
