// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 客服页 ViewModel
//
// 字幕浮层与 AI 状态指示都来自 AIConversationClient 的回调（由 AIConversationViewModel 汇总）：
//  - 字幕：latestUserText / latestAiText，随 onSubtitle 流式更新；
//  - AI 状态：agentState 映射为文案与是否播放波形动画。

package com.tencent.trtc.aiconversation.app.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.tencent.trtc.aiconversation.app.config.AppConfig
import com.tencent.trtc.aiconversation.AIConversationDef.AgentState

/** AI 客服页客服专用音色，仅本页使用。 */
private const val TTS_PARAMS_KEFU =
    "{\"model\":\"flow_02_turbo\",\"voice_id\":\"female-kefu-xiaomei\"," +
        "\"speed\":1.0,\"volume\":1.0,\"pitch\":0.0}"

class CustomerServiceViewModel : AIConversationViewModel() {

    /** AI 客服场景使用专属提示词，与通用助手区分。 */
    override val llmSystemPrompt: String
        get() = AppConfig.LLM_CUSTOMER_SERVICE_SYSTEM_PROMPT

    /** AI 客服场景的欢迎语。 */
    override val welcomeMessage: String? = "我是您的在线客服，有什么可以帮到您的？"

    /** AI 客服场景使用客服专用音色。 */
    override val ttsParams: String
        get() = TTS_PARAMS_KEFU

    /** 实时字幕是否开启，进入页面时默认关闭。 */
    var subtitleOn by mutableStateOf(false)
        private set

    /** 状态指示下方的文案，由机器人状态驱动。 */
    val statusLabel: String
        get() = when (agentState) {
            AgentState.LISTENING -> "AI 聆听中"
            AgentState.THINKING -> "AI 思考中"
            AgentState.SPEAKING -> "AI 回复中"
            AgentState.INTERRUPTED -> "已打断"
            else -> "AI 待命"
        }

    /** 思考 / 回复中波形条起伏，其余状态静止。 */
    val statusActive: Boolean
        get() = agentState == AgentState.THINKING || agentState == AgentState.SPEAKING

    /** 进房时 SDK 已自动订阅字幕流，这里只控制浮层显隐。 */
    fun toggleSubtitle() {
        subtitleOn = !subtitleOn
    }

    override fun reset() {
        super.reset()
        subtitleOn = false
    }
}
