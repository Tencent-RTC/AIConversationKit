package com.trtc.uikit.aiconversationkit.store

import kotlinx.coroutines.flow.StateFlow
import java.io.Serializable

enum class AIStatus(val value: Int) {
    INITIALIZING(0),
    LISTENING(1),
    THINKING(2),
    SPEAKING(3),
    INTERRUPTED(4),
    COMPLETED(5),
    OFFLINE(6),
}

data class AgentConfig (
    var aiRobotId: String = "", // Required field
    var aiRobotSig: String = "", // Required field
    var interruptMode: Int = 0,
    var welcomeMessage: String = "",
): Serializable

data class STTConfig (
    var language: String = "zh",
    var vadLevel: Int = 2,
): Serializable

data class AIConversationConfig(
    var secretId: String = "", // Required field
    var secretKey: String = "", // Required field
    var agentConfig: AgentConfig = AgentConfig(), // Required field
    var sttConfig: STTConfig = STTConfig(),
    var llmConfig: String = "", // Required field
    var ttsConfig: String = "", // Required field
    var region: String = "ap-beijing",
    var experimentalParams: String = "",
): Serializable

data class ConversationMessage(
    var roundId: String = "",
    var userSpeechText: String = "",
    var aiSpeechText: String = "",
    var timestamp: Long = 0L,
    var isCompleted: Boolean = false
): Serializable

data class ConversationState(
    val conversationMessageList: StateFlow<List<ConversationMessage>>,
    var aiStatus: StateFlow<AIStatus>,
    var isMicOpened: StateFlow<Boolean>,
): Serializable

abstract class AIConversationStore {
    companion object {
        @JvmField
        val shared: AIConversationStore = AIConversationStoreImpl.shared
    }

    abstract val conversationState: ConversationState

    abstract fun startAIConversation(config: AIConversationConfig?, completion: CompletionHandler?)
    abstract fun stopAIConversation(completion: CompletionHandler?)

    abstract fun interruptSpeech()
    abstract fun openLocalMicrophone(completion: CompletionHandler?)
    abstract fun closeLocalMicrophone()
}

interface CompletionHandler {
    fun onSuccess()

    fun onFailure(
        code: Int,
        desc: String
    )
}
