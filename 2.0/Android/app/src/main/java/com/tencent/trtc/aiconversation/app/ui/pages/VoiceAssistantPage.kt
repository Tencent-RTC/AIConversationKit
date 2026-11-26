// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 助手页

package com.tencent.trtc.aiconversation.app.ui.pages

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.rtmp.ui.TXCloudVideoView
import com.tencent.trtc.aiconversation.app.R
import com.tencent.trtc.aiconversation.app.ui.components.BindAIConversationSession
import com.tencent.trtc.aiconversation.app.ui.components.ControlButton
import com.tencent.trtc.aiconversation.app.ui.components.ControlStyle
import com.tencent.trtc.aiconversation.app.ui.components.NavBar
import com.tencent.trtc.aiconversation.app.ui.components.SubtitleButton
import com.tencent.trtc.aiconversation.app.ui.components.SwitchCameraButton
import com.tencent.trtc.aiconversation.app.ui.components.rememberPulse
import com.tencent.trtc.aiconversation.app.ui.theme.BackgroundColor
import com.tencent.trtc.aiconversation.app.ui.theme.Brand
import com.tencent.trtc.aiconversation.app.ui.theme.Bubble
import com.tencent.trtc.aiconversation.app.ui.theme.BubbleInk
import com.tencent.trtc.aiconversation.app.ui.theme.BubbleLine
import com.tencent.trtc.aiconversation.app.ui.theme.Ink
import com.tencent.trtc.aiconversation.app.ui.theme.LineColor
import com.tencent.trtc.aiconversation.app.ui.theme.Muted
import com.tencent.trtc.aiconversation.app.viewmodel.VoiceAssistantViewModel
import com.tencent.trtc.aiconversation.app.viewmodel.VoiceMessage

private const val TITLE = "AI 助手"

/**
 * AI 助手页。摄像头 / 字幕开关决定舞台区展示聊天记录、能量球还是实时文本卡，
 * 状态与交互逻辑见 [VoiceAssistantViewModel]。
 *
 * @param onEnd 结束对话后回调（点「结束对话」或返回），由上层导航返回首页
 */
@Composable
fun VoiceAssistantPage(
    onEnd: () -> Unit,
    viewModel: VoiceAssistantViewModel = viewModel()
) {
    // 返回键 / 返回手势与「结束对话」按钮一致：直接结束并回首页，不弹确认
    val endCall: () -> Unit = {
        viewModel.reset()
        onEnd()
    }
    BackHandler(onBack = endCall)
    BindAIConversationSession(viewModel)

    // 摄像头权限在首次打开时才申请；拒绝授权则保持关闭，只记日志不弹提示
    val context = LocalContext.current
    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.toggleCamera()
        } else {
            Log.w("VoiceAssistantPage", "CAMERA 未授权，无法开启本地摄像头")
        }
    }
    val onToggleCamera: () -> Unit = {
        if (viewModel.cameraOn || hasCameraPermission(context)) {
            viewModel.toggleCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        // 摄像头开：本地摄像头画面铺在最底层，底部渐隐到背景色以衬托控制区
        if (viewModel.cameraOn) {
            CameraPreview(viewModel = viewModel, modifier = Modifier.fillMaxSize())
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(284.dp)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color(0x00F5F7FB), Color(0xD1F5F7FB))
                        )
                    )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            NavBar(title = TITLE) {
                // 切换摄像头入口只在摄像头开启时出现，位于「字」按钮左侧
                if (viewModel.cameraOn) {
                    SwitchCameraButton(onClick = viewModel::switchCamera)
                }
                SubtitleButton(on = viewModel.subtitleOn, onClick = viewModel::toggleSubtitle)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                when {
                    viewModel.showChat -> ChatList(
                        messages = viewModel.messages,
                        typing = viewModel.talking
                    )
                    viewModel.showOrb -> VoiceOrb(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 87.dp)
                    )
                    viewModel.showCompact -> CompactCard(
                        user = viewModel.latestUserText,
                        ai = viewModel.latestAiText,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 328.dp)
                    )
                }
            }

            InterruptButton(
                text = viewModel.statusText,
                talking = viewModel.talking,
                onClick = viewModel::toggleInterrupt,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
            ) {
                ControlButton(
                    label = if (viewModel.micOn) "麦克风开" else "已静音",
                    icon = if (viewModel.micOn) Icons.Default.Mic else Icons.Default.MicOff,
                    style = if (viewModel.micOn) ControlStyle.NORMAL else ControlStyle.OFF,
                    onClick = viewModel::toggleMic
                )
                ControlButton(
                    label = if (viewModel.cameraOn) "摄像头开" else "摄像头已关",
                    icon = if (viewModel.cameraOn) Icons.Default.Videocam else Icons.Default.VideocamOff,
                    style = if (viewModel.cameraOn) ControlStyle.NORMAL else ControlStyle.OFF,
                    onClick = onToggleCamera
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


/* ---------------------------------------------------
 * 本地摄像头预览
 * --------------------------------------------------- */

private fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * 本地摄像头画面。进入组合时开启预览，离开（关闭摄像头或离开页面）时关闭。
 * TXCloudVideoView 必须在预览期间保持挂载，所以用 remember 持有同一个实例。
 */
@Composable
private fun CameraPreview(viewModel: VoiceAssistantViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val previewView = remember { TXCloudVideoView(context) }
    AndroidView(factory = { previewView }, modifier = modifier)
    DisposableEffect(previewView) {
        viewModel.startCameraPreview(previewView)
        onDispose { viewModel.stopCameraPreview() }
    }
}


/* ---------------------------------------------------
 * 聊天记录
 * --------------------------------------------------- */

@Composable
private fun ChatList(messages: List<VoiceMessage>, typing: Boolean) {
    val scrollState = rememberScrollState()

    // 字幕是流式更新的（同一条记录的 text 会变长），新内容到来时自动滚到底部
    val lastText = messages.lastOrNull()?.text
    LaunchedEffect(messages.size, lastText) {
        if (messages.isNotEmpty()) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        if (messages.isEmpty() && !typing) {
            Text(
                text = "说点什么，和 AI 聊聊天吧",
                fontSize = 14.sp,
                color = Muted,
                modifier = Modifier.padding(top = 16.dp)
            )
        }

        messages.forEach { msg ->
            // 未结束的字幕追加光标，表示该句仍在流式输出
            val displayText = if (!msg.isFinal && !msg.fromMe) msg.text + " ▍" else msg.text
            if (msg.fromMe) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = displayText,
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = BubbleInk,
                        modifier = Modifier
                            .widthIn(max = 290.dp)
                            .background(
                                Bubble,
                                RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp
                                )
                            )
                            .border(
                                1.dp, BubbleLine,
                                RoundedCornerShape(
                                    topStart = 18.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 4.dp
                                )
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    )
                }
            } else {
                Text(text = displayText, fontSize = 14.sp, lineHeight = 22.sp, color = Ink)
            }
        }
        if (typing) TypingDots()
    }
}

/** AI 说话中的三点跳动动画。 */
@Composable
private fun TypingDots() {
    val transition = rememberInfiniteTransition(label = "typing")
    Row(
        modifier = Modifier.height(22.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(550),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = androidx.compose.animation.core.StartOffset(index * 160)
                ),
                label = "dot$index"
            )
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .background(Brand.copy(alpha = alpha), CircleShape)
            )
        }
    }
}


/* ---------------------------------------------------
 * 纯语音活跃态：能量球
 * --------------------------------------------------- */

@Composable
private fun VoiceOrb(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "orb")
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.015f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "orbScale"
    )
    Image(
        painter = painterResource(R.drawable.voice_orb),
        contentDescription = "AI 对话活跃态",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(300.dp)
            .scale(scale)
            .clip(CircleShape)
    )
}


/* ---------------------------------------------------
 * 摄像头开 + 字幕开：紧凑实时文本卡
 * --------------------------------------------------- */

@Composable
private fun CompactCard(user: String, ai: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(20.dp))
            .border(1.dp, LineColor, RoundedCornerShape(20.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "实时文本 · VoiceAI 正在说话",
            fontSize = 11.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Brand
        )
        Text(text = "你：$user", fontSize = 12.sp, lineHeight = 20.sp, color = Muted)
        Text(text = ai, fontSize = 14.sp, lineHeight = 22.sp, color = Ink)
    }
}


/* ---------------------------------------------------
 * 说话或点击打断
 * --------------------------------------------------- */

@Composable
private fun InterruptButton(
    text: String,
    talking: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(36.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (talking) {
                val p = rememberPulse(1400)
                Box(
                    modifier = Modifier
                        .size(17.dp)
                        .scale(1f + 1.1f * p)
                        .background(Brand.copy(alpha = 0.45f * (1f - p)), CircleShape)
                )
            }
            Box(
                modifier = Modifier
                    .size(17.dp)
                    .background(if (talking) Brand else Color(0xFFC6C4E4), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .background(Color(0xFFF7F6FF), RoundedCornerShape(1.dp))
                )
            }
        }
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF636B86)
        )
    }
}
