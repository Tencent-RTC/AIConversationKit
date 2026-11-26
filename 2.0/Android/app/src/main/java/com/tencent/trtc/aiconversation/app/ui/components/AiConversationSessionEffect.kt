// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 通话页权限申请与 AIConversationClient 会话的生命周期绑定

package com.tencent.trtc.aiconversation.app.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.tencent.trtc.aiconversation.app.viewmodel.AIConversationViewModel
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "BindAIConversationSession"

/**
 * 把通话页的出现 / 消失绑定到权限申请与 [AIConversationViewModel] 会话生命周期：
 *
 * - 进入页面时才申请录音权限（进房成功后 startLocalAudio 需要），拿到结果后再进房；
 * - 离开页面（结束对话、返回首页、Activity 销毁）时注销监听并退房。
 *
 * Manifest 里只有 RECORD_AUDIO 是危险权限；首页不会弹权限框。
 * AIConversationClient 是单例且监听器全局唯一，必须由页面显式释放，否则会继续
 * 收到回调并持有页面对应的 ViewModel。
 */
@Composable
fun BindAIConversationSession(viewModel: AIConversationViewModel) {
    val context = LocalContext.current
    val appContext = context.applicationContext

    // 权限对话框可能比页面存活更久（返回键 / 结束对话），用标志位丢弃迟到结果，
    // 避免退房后又发起一次进房。
    val active = remember { AtomicBoolean(false) }

    val requestRecordAudio = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!active.get()) {
            Log.d(TAG, "页面已离开，忽略迟到的权限结果")
            return@rememberLauncherForActivityResult
        }
        if (!granted) {
            Log.w(TAG, "RECORD_AUDIO 未授权，进房后无法采集本地语音")
        }
        // 拒绝授权也继续进房，只是本地音频不可用；用户可在系统设置里补授权
        viewModel.onPageEnter(appContext)
    }

    DisposableEffect(Unit) {
        active.set(true)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            viewModel.onPageEnter(appContext)
        } else {
            requestRecordAudio.launch(Manifest.permission.RECORD_AUDIO)
        }
        onDispose {
            active.set(false)
            viewModel.onPageLeave()
        }
    }
}
