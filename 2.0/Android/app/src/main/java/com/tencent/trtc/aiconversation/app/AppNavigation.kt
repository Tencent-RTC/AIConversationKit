// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - Navigation

package com.tencent.trtc.aiconversation.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tencent.trtc.aiconversation.app.ui.pages.CustomerServicePage
import com.tencent.trtc.aiconversation.app.ui.pages.CompanionPage
import com.tencent.trtc.aiconversation.app.ui.pages.HomePage
import com.tencent.trtc.aiconversation.app.ui.pages.SplashPage
import com.tencent.trtc.aiconversation.app.ui.pages.VoiceAssistantPage
import com.tencent.trtc.aiconversation.app.viewmodel.HomeViewModel

/**
 * 单 Activity 下的页面路由。使用字符串状态机 + AnimatedContent 做页面切换。
 *
 * 首页三张卡片分别进入 AI 客服 / AI 陪伴 / AI 助手通话页；
 * 通话页没有返回按钮，「结束对话」确认后（或系统返回键触发确认后）回到首页。
 */
object Routes {
    const val HOME = "home"
    const val AGENT = "agent"          // AI 客服
    const val COMPANION = "companion"  // AI 陪伴
    const val VOICE = "voice"          // AI 助手
}

@Composable
fun AppNavigation() {
    var screen by rememberSaveable { mutableStateOf(Routes.HOME) }
    // 启动页只在冷启动时展示；rememberSaveable 保证旋转等重建不会重复出现
    var showSplash by rememberSaveable { mutableStateOf(true) }
    val homeViewModel: HomeViewModel = viewModel()

    val backToHome: () -> Unit = {
        screen = Routes.HOME
    }

    if (showSplash) {
        SplashPage(onFinished = { showSplash = false })
    } else {
        AnimatedContent(
            targetState = screen,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "nav"
        ) { dest ->
            when (dest) {
                Routes.AGENT -> CustomerServicePage(onEnd = backToHome)
                Routes.COMPANION -> CompanionPage(onEnd = backToHome)
                Routes.VOICE -> VoiceAssistantPage(onEnd = backToHome)
                else -> HomePage(
                    viewModel = homeViewModel,
                    onAgent = { screen = Routes.AGENT },
                    onCompanion = { screen = Routes.COMPANION },
                    onVoice = { screen = Routes.VOICE }
                )
            }
        }
    }
}
