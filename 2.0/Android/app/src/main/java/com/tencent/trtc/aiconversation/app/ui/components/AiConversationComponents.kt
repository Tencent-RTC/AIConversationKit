// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 通话页共用组件（导航栏 / 控制按钮 / 字幕开关 / AI 状态指示）

package com.tencent.trtc.aiconversation.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tencent.trtc.aiconversation.app.ui.theme.Brand
import com.tencent.trtc.aiconversation.app.ui.theme.BrandInk
import com.tencent.trtc.aiconversation.app.ui.theme.BrandSoft
import com.tencent.trtc.aiconversation.app.ui.theme.BrandSoftLine
import com.tencent.trtc.aiconversation.app.ui.theme.Danger
import com.tencent.trtc.aiconversation.app.ui.theme.Ink
import com.tencent.trtc.aiconversation.app.ui.theme.Muted
import com.tencent.trtc.aiconversation.app.ui.theme.OffBg
import com.tencent.trtc.aiconversation.app.ui.theme.OffLine

/** 循环脉冲进度 0f..1f，用于呼吸光圈。 */
@Composable
fun rememberPulse(durationMs: Int = 1400): Float {
    val transition = rememberInfiniteTransition(label = "pulse")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMs, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseProgress"
    )
    return progress
}

/** 通话页顶部导航栏：居中标题 + 可选的右侧控件。 */
@Composable
fun NavBar(
    title: String,
    trailing: (@Composable BoxScope.() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink,
            modifier = Modifier.align(Alignment.Center)
        )
        trailing?.invoke(this)
    }
}

/** 导航栏右上角「字」字幕开关。 */
@Composable
fun BoxScope.SubtitleButton(
    on: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .size(30.dp)
            .clip(CircleShape)
            .background(if (on) BrandSoft else OffBg)
            .border(1.dp, if (on) BrandSoftLine else OffLine, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "字",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = if (on) BrandInk else Muted
        )
        if (on) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = (-3).dp, y = (-3).dp)
                    .size(11.dp)
                    .background(Brand, CircleShape)
                    .border(1.5.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(7.dp)
                )
            }
        }
    }
}

/**
 * 导航栏「切换前后摄像头」按钮，位于「字」按钮左侧（30dp 按钮 + 8dp 间距）。
 * 仅在摄像头开启时才应显示。
 */
@Composable
fun BoxScope.SwitchCameraButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .align(Alignment.CenterEnd)
            .offset(x = (-38).dp)
            .size(30.dp)
            .clip(CircleShape)
            .background(OffBg)
            .border(1.dp, OffLine, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlipCameraAndroid,
            contentDescription = "切换前后摄像头",
            tint = Muted,
            modifier = Modifier.size(18.dp)
        )
    }
}

/** 圆形控制按钮的样式。 */
enum class ControlStyle { NORMAL, OFF, DANGER }

/**
 * 通话页底部圆形控制按钮（64dp 圆 + 下方文字标签）。
 *
 * @param dangerIconTint 仅 [ControlStyle.DANGER] 使用，默认白色；AI 陪伴页按设计稿使用深色。
 */
@Composable
fun ControlButton(
    label: String,
    icon: ImageVector,
    style: ControlStyle,
    onClick: () -> Unit,
    dangerIconTint: Color = Color.White
) {
    val knobBg: Color
    val knobBorder: Color
    val iconTint: Color
    val labelColor: Color
    when (style) {
        ControlStyle.NORMAL -> {
            knobBg = BrandSoft; knobBorder = BrandSoft; iconTint = Color(0xFF0B1621); labelColor = Brand
        }
        ControlStyle.OFF -> {
            knobBg = OffBg; knobBorder = OffLine; iconTint = Color(0xFF7A8397); labelColor = Muted
        }
        ControlStyle.DANGER -> {
            knobBg = Danger; knobBorder = Danger; iconTint = dangerIconTint; labelColor = Muted
        }
    }

    Column(
        modifier = Modifier
            .width(80.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(knobBg)
                .border(BorderStroke(1.dp, knobBorder), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(26.dp)
            )
        }
        Text(text = label, fontSize = 11.sp, color = labelColor)
    }
}

private val AiStatusColor = Color(0xFF286CAA)

/** 波形条静止时的纵向缩放系数，与动画下限一致，切换时不会跳变。 */
private const val WAVE_BAR_REST_SCALE = 0.55f

/**
 * AI 客服 AI 状态指示：非按钮，不可点击。
 * 实心蓝色对话气泡 + 三根白色波形条，下方为文字标签。
 *
 * @param active true 时波形条上下起伏（AI 思考 / 回复中），false 时静止
 */
@Composable
fun AiStatusIndicator(label: String, active: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "aiStatus")
    // 三根波形条依次错开 160ms，1.2s 一个来回周期
    val animatedBar1 by waveBar(transition, delayMs = 0)
    val animatedBar2 by waveBar(transition, delayMs = 160)
    val animatedBar3 by waveBar(transition, delayMs = 320)
    val bar1 = if (active) animatedBar1 else WAVE_BAR_REST_SCALE
    val bar2 = if (active) animatedBar2 else WAVE_BAR_REST_SCALE
    val bar3 = if (active) animatedBar3 else WAVE_BAR_REST_SCALE

    Column(
        modifier = modifier.width(80.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(width = 40.dp, height = 30.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(28.dp)) {
                // 设计稿基于 28 x 28 的 viewBox，按实际尺寸等比映射
                val u = size.width / 28f

                drawRoundRect(
                    color = AiStatusColor,
                    topLeft = Offset(4f * u, 7f * u),
                    size = Size(21f * u, 17f * u),
                    cornerRadius = CornerRadius(6f * u, 6f * u)
                )
                // 气泡左下角的小尾巴
                drawPath(
                    path = Path().apply {
                        moveTo(8.5f * u, 21.5f * u)
                        lineTo(8.5f * u, 26.7f * u)
                        lineTo(13.7f * u, 23.1f * u)
                        close()
                    },
                    color = AiStatusColor
                )

                // 波形条：以各自中心为轴做纵向缩放
                fun bar(x: Float, y: Float, h: Float, scaleY: Float) {
                    val cy = (y + h / 2f) * u
                    val height = h * u * scaleY
                    drawRoundRect(
                        color = Color.White,
                        topLeft = Offset(x * u, cy - height / 2f),
                        size = Size(1.8f * u, height),
                        cornerRadius = CornerRadius(0.9f * u, 0.9f * u)
                    )
                }
                bar(9.8f, 13.4f, 4.2f, bar1)
                bar(13.8f, 11.4f, 8.2f, bar2)
                bar(17.8f, 13.4f, 4.2f, bar3)
            }
        }
        Text(text = label, fontSize = 11.sp, color = AiStatusColor)
    }
}

/** 波形条的纵向缩放系数，在 0.55..1 之间往复。 */
@Composable
private fun waveBar(
    transition: androidx.compose.animation.core.InfiniteTransition,
    delayMs: Int
) = transition.animateFloat(
    initialValue = WAVE_BAR_REST_SCALE,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
        animation = tween(600, easing = FastOutSlowInEasing),
        repeatMode = RepeatMode.Reverse,
        initialStartOffset = StartOffset(delayMs)
    ),
    label = "waveBar$delayMs"
)
