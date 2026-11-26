// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 客服页

package com.tencent.trtc.aiconversation.app.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.trtc.aiconversation.app.R
import com.tencent.trtc.aiconversation.app.ui.components.AiStatusIndicator
import com.tencent.trtc.aiconversation.app.ui.components.BindAIConversationSession
import com.tencent.trtc.aiconversation.app.ui.components.ControlButton
import com.tencent.trtc.aiconversation.app.ui.components.ControlStyle
import com.tencent.trtc.aiconversation.app.ui.components.NavBar
import com.tencent.trtc.aiconversation.app.ui.components.SubtitleButton
import com.tencent.trtc.aiconversation.app.ui.theme.BackgroundColor
import com.tencent.trtc.aiconversation.app.viewmodel.CustomerServiceViewModel

private const val TITLE = "AI 客服"

/**
 * AI 客服页：数字人画面 + 底部实时字幕，字幕可通过右上角「字」按钮开关。
 *
 * @param onEnd 结束对话后回调（点「结束对话」或返回），由上层导航返回首页
 */
@Composable
fun CustomerServicePage(
    onEnd: () -> Unit,
    viewModel: CustomerServiceViewModel = viewModel()
) {
    // 返回键 / 返回手势与「结束对话」按钮一致：直接结束并回首页，不弹确认
    val endCall: () -> Unit = {
        viewModel.reset()
        onEnd()
    }
    BackHandler(onBack = endCall)
    BindAIConversationSession(viewModel)

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
        ) {
            NavBar(title = TITLE) {
                SubtitleButton(on = viewModel.subtitleOn, onClick = viewModel::toggleSubtitle)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(24.dp))
            ) {
                Image(
                    painter = painterResource(R.drawable.agent_cam),
                    contentDescription = "AI 客服画面",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                SubtitleOverlay(
                    visible = viewModel.subtitleOn,
                    user = viewModel.latestUserText,
                    agent = viewModel.latestAiText
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
            ) {
                ControlButton(
                    label = if (viewModel.micOn) "麦克风开" else "已静音",
                    icon = if (viewModel.micOn) Icons.Default.Mic else Icons.Default.MicOff,
                    style = if (viewModel.micOn) ControlStyle.NORMAL else ControlStyle.OFF,
                    onClick = viewModel::toggleMic
                )
                // 状态指示，非按钮；与两侧按钮垂直居中对齐，文案与动画随 agentState 变化
                AiStatusIndicator(
                    label = viewModel.statusLabel,
                    active = viewModel.statusActive,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
                ControlButton(
                    label = "结束对话",
                    icon = Icons.Default.CallEnd,
                    style = ControlStyle.DANGER,
                    onClick = endCall
                )
            }
        }
    }
}

/** 画面底部的字幕浮层，带淡入淡出 + 上下位移动画。 */
@Composable
private fun SubtitleOverlay(visible: Boolean, user: String, agent: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically { it / 4 },
            exit = fadeOut() + slideOutVertically { it / 4 }
        ) {
            Subtitle(user = user, agent = agent, modifier = Modifier.padding(16.dp))
        }
    }
}

/** 画面底部的深色实时字幕面板：用户语音识别 + AI 回复。 */
@Composable
private fun Subtitle(user: String, agent: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xE00D1726), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = "实时字幕",
            fontSize = 11.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFB9DFFC)
        )
        SubtitleLine(role = "我 · 语音识别", text = user, background = Color(0xFF294052), roleColor = Color(0xFFB8DDF7))
        SubtitleLine(role = "AI 客服 · 回复", text = agent, background = Color(0xFF204C6C), roleColor = Color(0xFFB8E4FF))
    }
}

@Composable
private fun SubtitleLine(role: String, text: String, background: Color, roleColor: Color) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(text = role, fontSize = 11.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium, color = roleColor)
        Text(text = text, fontSize = 13.sp, lineHeight = 21.sp, color = Color.White)
    }
}
