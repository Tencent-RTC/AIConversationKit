// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 陪伴页

package com.tencent.trtc.aiconversation.app.ui.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.trtc.aiconversation.app.R
import com.tencent.trtc.aiconversation.app.ui.components.BindAIConversationSession
import com.tencent.trtc.aiconversation.app.ui.components.ControlButton
import com.tencent.trtc.aiconversation.app.ui.components.ControlStyle
import com.tencent.trtc.aiconversation.app.ui.theme.BackgroundColor
import com.tencent.trtc.aiconversation.app.viewmodel.CompanionViewModel

/**
 * AI 陪伴页：全屏数字人画面铺底，只在底部悬浮麦克风与结束对话两个按钮。
 *
 * @param onEnd 结束对话后回调（点「结束对话」或返回），由上层导航返回首页
 */
@Composable
fun CompanionPage(
    onEnd: () -> Unit,
    viewModel: CompanionViewModel = viewModel()
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
        Image(
            painter = painterResource(R.drawable.companion_full),
            contentDescription = "AI 陪伴画面",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(56.dp, Alignment.CenterHorizontally)
        ) {
            ControlButton(
                label = if (viewModel.micOn) "麦克风开" else "已静音",
                icon = if (viewModel.micOn) Icons.Default.Mic else Icons.Default.MicOff,
                style = if (viewModel.micOn) ControlStyle.NORMAL else ControlStyle.OFF,
                onClick = viewModel::toggleMic
            )
            ControlButton(
                label = "结束对话",
                icon = Icons.Default.CallEnd,
                style = ControlStyle.DANGER,
                onClick = endCall,
                dangerIconTint = Color(0xFF241913)
            )
        }
    }
}
