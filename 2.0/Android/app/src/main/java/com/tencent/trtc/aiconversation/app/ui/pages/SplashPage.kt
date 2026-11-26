// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 启动页

package com.tencent.trtc.aiconversation.app.ui.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.tencent.trtc.aiconversation.app.R
import kotlinx.coroutines.delay

/** 启动页停留时长。 */
private const val SPLASH_DURATION_MS = 1800L

/**
 * 启动页：全屏铺满启动背景图，停留片刻后回调进入首页。
 * 背景放在 drawable-nodpi 并用 Crop，适配不同屏幕比例。
 */
@Composable
fun SplashPage(onFinished: () -> Unit) {
    Image(
        painter = painterResource(R.drawable.splash_bg),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize()
    )
    LaunchedEffect(Unit) {
        delay(SPLASH_DURATION_MS)
        onFinished()
    }
}
