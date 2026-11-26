// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 陪伴页 ViewModel
//
// 全屏数字人画面，只有麦克风与结束对话，无额外状态，全部复用 [AIConversationViewModel]。
// 本页使用端到端（S2S）模式创建 AI 任务。

package com.tencent.trtc.aiconversation.app.viewmodel

import com.tencent.trtc.aiconversation.app.config.AppConfig

class CompanionViewModel : AIConversationViewModel() {

    /** AI 陪伴使用端到端模式。 */
    override val useS2S: Boolean = true

    /** AI 陪伴场景使用专属提示词，与通用助手区分。 */
    override val llmSystemPrompt: String
        get() = AppConfig.LLM_COMPANION_SYSTEM_PROMPT
}
