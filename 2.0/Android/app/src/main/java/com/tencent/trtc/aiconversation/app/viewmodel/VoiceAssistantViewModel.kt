// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 助手页 ViewModel
//
// 摄像头 / 字幕两个开关组合出四种布局：
//  - 摄像头关 + 字幕开：实时字幕聊天记录；
//  - 摄像头关 + 字幕关：纯语音活跃态能量球；
//  - 摄像头开 + 字幕开：全屏摄像头画面 + 紧凑的实时文本卡；
//  - 摄像头开 + 字幕关：仅全屏摄像头画面。
// 聊天内容来自 AIConversationClient 的 onSubtitle 回调（由 AIConversationViewModel 汇总），
// 不再是静态示例。

package com.tencent.trtc.aiconversation.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.trtc.aiconversation.AIConversationDef.AgentState

class VoiceAssistantViewModel : AIConversationViewModel() {

    override val welcomeMessage: String? = "你好，我是 AI 助手，有什么可以帮你？"

    /** 摄像头是否开启。 */
    var cameraOn by mutableStateOf(false)
        private set

    /** 字幕 / 实时文本是否开启（仅控制 UI；SDK 没有取消订阅接口）。 */
    var subtitleOn by mutableStateOf(true)
        private set

    /** AI 是否正在播报，由机器人状态驱动，可点击打断。 */
    val talking: Boolean get() = agentState == AgentState.SPEAKING

    /** 底部状态条文案，随机器人状态变化；AI 说话时提示可以说话或点击打断。 */
    val statusText: String
        get() = when (agentState) {
            AgentState.SPEAKING -> "说话或点击打断"
            AgentState.LISTENING -> "正在聆听"
            AgentState.THINKING -> "正在思考"
            AgentState.INTERRUPTED -> "已打断"
            else -> "AI 待命"
        }

    val showChat: Boolean get() = !cameraOn && subtitleOn
    val showOrb: Boolean get() = !cameraOn && !subtitleOn
    val showCompact: Boolean get() = cameraOn && subtitleOn

    fun toggleCamera() {
        cameraOn = !cameraOn
    }

    fun toggleSubtitle() {
        subtitleOn = !subtitleOn
    }

    /** 点击打断：交给 SDK 发打断指令，状态由 onAgentStateChanged 回调更新。 */
    fun toggleInterrupt() {
        if (talking) requestInterrupt()
    }

    override fun reset() {
        super.reset()
        cameraOn = false
        subtitleOn = true
    }
}
