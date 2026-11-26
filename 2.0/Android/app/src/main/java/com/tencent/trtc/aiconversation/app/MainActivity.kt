// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 单 Activity 入口

package com.tencent.trtc.aiconversation.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.tencent.trtc.aiconversation.app.ui.theme.AIConversationTheme
import com.tencent.trtc.aiconversation.app.ui.theme.BackgroundColor

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 沉浸式：全屏数字人 / 摄像头画面铺到系统栏之下，系统栏透明、图标为深色
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )

        setContent {
            AIConversationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BackgroundColor
                ) {
                    AppNavigation()
                }
            }
        }
    }
}
