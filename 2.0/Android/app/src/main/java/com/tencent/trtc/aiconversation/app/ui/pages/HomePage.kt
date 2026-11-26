// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 首页

package com.tencent.trtc.aiconversation.app.ui.pages

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.trtc.aiconversation.app.R
import com.tencent.trtc.aiconversation.app.ui.components.Toast
import com.tencent.trtc.aiconversation.app.viewmodel.HomeViewModel
import com.tencent.trtc.aiconversation.app.ui.theme.BackgroundColor
import com.tencent.trtc.aiconversation.app.ui.theme.Brand
import com.tencent.trtc.aiconversation.app.ui.theme.BrandSoft
import com.tencent.trtc.aiconversation.app.ui.theme.Ink
import com.tencent.trtc.aiconversation.app.ui.theme.LineColor
import com.tencent.trtc.aiconversation.app.ui.theme.Muted

/**
 * 首页：品牌标题 + 三张功能卡片（AI 客服 / AI 陪伴 / AI 助手）。
 * 点击事件通过回调交给上层导航处理。
 */
@Composable
fun HomePage(
    viewModel: HomeViewModel = viewModel(),
    onAgent: () -> Unit = {},
    onCompanion: () -> Unit = {},
    onVoice: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            BrandRow()

            Welcome()

            AgentCard(onClick = onAgent)

            CompanionCard(onClick = onCompanion)

            VoiceCard(onClick = onVoice)
        }

        Toast(message = viewModel.toast, onDismiss = viewModel::dismissToast)
    }
}


/* ---------------------------------------------------
 * Header
 * --------------------------------------------------- */

@Composable
private fun BrandRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "TRTC AI Conversation",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )
    }
}

@Composable
private fun Welcome() {
    Column(
        modifier = Modifier.padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "不止对话，更有陪伴。",
            fontSize = 25.sp,
            lineHeight = 36.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
        Text(
            text = "让智能有回应，让陪伴有温度",
            fontSize = 13.sp,
            lineHeight = 22.sp,
            color = Muted
        )
    }
}


/* ---------------------------------------------------
 * Cards
 * --------------------------------------------------- */

private val CardShape = RoundedCornerShape(28.dp)

/** 卡片内的标题 / 描述 / 标签三行文案。 */
@Composable
private fun CardTexts(title: String, desc: String?, tag: String) {
    Column(modifier = Modifier.padding(start = 20.dp, top = 19.dp)) {
        Text(
            text = title,
            fontSize = 20.sp,
            lineHeight = 32.sp,
            fontWeight = FontWeight.SemiBold,
            color = Ink
        )
        if (desc != null) {
            Spacer(modifier = Modifier.height(7.dp))
            Text(text = desc, fontSize = 12.sp, lineHeight = 20.sp, color = Muted)
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            Spacer(modifier = Modifier.height(1.dp))
        }
        Text(text = tag, fontSize = 11.sp, lineHeight = 18.sp, color = Brand)
    }
}

/** 卡片底部的胶囊按钮。 */
@Composable
private fun BoxScope.CardCta(
    onClick: () -> Unit,
    ghost: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Center,
    content: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(start = 20.dp, end = 20.dp, bottom = 16.dp)
            .fillMaxWidth()
            .height(44.dp)
            .clip(CircleShape)
            .background(if (ghost) BrandSoft.copy(alpha = 0.92f) else BrandSoft)
            .clickable(onClick = onClick)
            .padding(horizontal = if (ghost) 16.dp else 0.dp),
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
private fun CtaText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF09131A)
    )
}

@Composable
private fun AgentCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, LineColor, CardShape)
    ) {
        CardTexts(title = "AI 客服", desc = "有问题随时问，贴心解答", tag = "随时解答 · 高效服务")

        Image(
            painter = painterResource(R.drawable.agent_avatar),
            contentDescription = "AI 客服",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 20.dp, top = 28.dp)
                .size(78.dp)
                .clip(CircleShape)
        )

        CardCta(onClick = onClick) { CtaText("立即咨询") }
    }
}

@Composable
private fun CompanionCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(CardShape)
            .background(Color(0xFFE7F4FF))
    ) {
        Image(
            painter = painterResource(R.drawable.companion_card),
            contentDescription = "AI 陪伴",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // 右侧渐隐遮罩，保证右侧文字可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0x1FE7F4FF),
                        0.48f to Color(0x00E7F4FF),
                        1f to Color(0xF0E7F4FF)
                    )
                )
        )

        CardTexts(title = "AI 陪伴 - HY Realtime", desc = null, tag = "真实神态 · 自然互动")

        Column(modifier = Modifier.padding(start = 20.dp, top = 98.dp)) {
            Text(
                text = "你的 AI 伙伴",
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                color = Ink
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "AI 生成形象，非真人",
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Muted
            )
        }

        CardCta(
            onClick = onClick,
            ghost = true,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            CtaText("与 Ta 面对面")
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = Color(0xFF09131A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun VoiceCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(CardShape)
            .background(Color.White)
            .border(1.dp, LineColor, CardShape)
    ) {
        CardTexts(title = "AI 助手", desc = "开口即聊，灵感实时回应", tag = "语音 · 自然交流")

        VoiceWave(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(end = 16.dp, top = 27.dp)
                .size(width = 106.dp, height = 79.dp)
        )

        CardCta(onClick = onClick) {
            CtaText("开始对话")
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = Color(0xFF09131A),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * AI 助手卡片右上角的声波装饰：淡蓝径向光晕 + 一组渐变圆头声波条。
 * 条高由中间向两端收窄，并随相位缓慢起伏，呼应「开口即聊」。
 */
@Composable
private fun VoiceWave(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "voiceWave")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "voiceWavePhase"
    )

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cy = h * 0.52f
        val center = Offset(w * 0.5f, cy)

        // 光晕：让装饰与白色卡片之间有一层柔和过渡
        val glowRadius = w * 0.46f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(
                    BrandSoft.copy(alpha = 0.42f),
                    BrandSoft.copy(alpha = 0f)
                ),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )

        val count = 11
        val step = w * 0.072f
        val startX = (w - step * (count - 1)) / 2f
        val endX = startX + step * (count - 1)
        val barWidth = w * 0.032f
        val maxHalf = h * 0.34f
        val accent = Color(0xFF6F63C9)

        // 两端淡、中心浓的对称渐变：主色沿用主题蓝，中心落一笔柔紫作为焦点
        val barBrush = Brush.horizontalGradient(
            colors = listOf(
                Brand.copy(alpha = 0.30f),
                Brand.copy(alpha = 0.90f),
                accent,
                Brand.copy(alpha = 0.90f),
                Brand.copy(alpha = 0.30f)
            ),
            startX = startX,
            endX = endX
        )

        for (i in 0 until count) {
            val t = i.toFloat() / (count - 1)
            val envelope = sin(PI.toFloat() * t).pow(0.85f)
            val ripple = 0.62f + 0.38f * sin(phase + i * 0.62f)
            val half = (maxHalf * envelope * ripple).coerceAtLeast(3f)
            val x = startX + step * i
            drawLine(
                brush = barBrush,
                start = Offset(x, cy - half),
                end = Offset(x, cy + half),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
                alpha = 0.35f + 0.65f * envelope
            )
        }
    }
}
