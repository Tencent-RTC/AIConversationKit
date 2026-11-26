// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - 通话页 ViewModel 基类
//
// 三个通话页（AI 助手 / AI 客服 / AI 陪伴）共有的状态：麦克风开关，
// 以及 AIConversationClient 会话接入：
//   onPageEnter()  进房（SDK 在进房成功后自动订阅字幕）
//   onConversationStarted(CODE_OK) → AIConversationRequest 直连云 API 创建 AI 任务
//   onPageLeave()  注销监听并退房、直连云 API 停止 AI 任务
//
// AI 任务由客户端直连腾讯云 API 3.0 创建（级联 / S2S），凭据与 LLM 配置取自 AppConfig，
// 场景提示词 / 欢迎语 / TTS 参数由各通话页覆盖。

package com.tencent.trtc.aiconversation.app.viewmodel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.tencent.rtmp.ui.TXCloudVideoView
import com.tencent.trtc.aiconversation.app.config.AppConfig
import com.tencent.trtc.aiconversation.app.debug.GenerateTestUserSig
import com.tencent.trtc.aiconversation.AIConversationClient
import com.tencent.trtc.aiconversation.AIConversationDef
import com.tencent.trtc.aiconversation.AIConversationDef.AgentState
import com.tencent.trtc.aiconversation.AIConversationDef.SubtitleItem
import com.tencent.trtc.aiconversation.AIConversationError
import com.tencent.trtc.aiconversation.AIConversationListener
import com.tencent.trtc.aiconversation.app.debug.AIConversationRequest

/**
 * 一条聊天记录。[key] 由字幕的 roundId + 角色生成，用于定位同一轮次；
 * [text] 是该行「截至当前的完整文本」，流式片段会拼接到这里。
 */
data class VoiceMessage(
    val key: String,
    val fromMe: Boolean,
    val text: String,
    val isFinal: Boolean = true
)

open class AIConversationViewModel : ViewModel() {

    companion object {
        private const val TAG = "AIConversationViewModel"

        private const val ROBOT_ID_SUFFIX = "_bot"
    }

    /**
     * 是否使用端到端（S2S）模式创建 AI 任务。默认 false 走级联；各通话页可覆盖。
     * 端到端只下发 S2SConfig，不下发 LLM / TTS 配置。
     */
    protected open val useS2S: Boolean = false

    /**
     * 场景 LLM 系统提示词，作为 LLMConfig.SystemPrompt 下发。
     * 各通话页按场景覆盖；未覆盖的页面沿用 AI 助手的通用提示词。
     */
    protected open val llmSystemPrompt: String
        get() = AppConfig.LLM_VOICE_ASSISTANT_SYSTEM_PROMPT

    /**
     * 场景欢迎语，作为 DialogConfig.WelcomeMessage 下发；默认 null 表示不下发。
     * 各通话页可覆盖。
     */
    protected open val welcomeMessage: String? = null

    /**
     * 场景 TTS 参数 JSON 字符串，作为 TTSConfig.Params 下发。
     * 各通话页按场景覆盖；未覆盖的页面沿用通用音色。
     */
    protected open val ttsParams: String
        get() = AIConversationRequest.TTS_PARAMS

    /** 麦克风是否开启。 */
    var micOn by mutableStateOf(true)
        private set

    /** 会话期间持有的单例引用；离开页面后置空，避免页面被 listener 隐式持有。 */
    private var client: AIConversationClient? = null
    private var sessionStarted = false

    /** 云 API 返回的 AI 任务 ID，供后续停止任务使用。 */
    var taskId: String? = null
        private set

    /** 实时字幕驱动的聊天记录；同一轮次（roundId + 角色）流式更新时原位替换。 */
    private val _messages = mutableStateListOf<VoiceMessage>()
    val messages: List<VoiceMessage> get() = _messages

    /** 机器人状态，用于字幕页展示「正在说话 / 思考中」。 */
    var agentState by mutableStateOf(AgentState.IDLE)
        private set

    /** 最近一轮的用户发言；还没有字幕时给占位文案。 */
    val latestUserText: String
        get() = _messages.lastOrNull { it.fromMe }?.text ?: "等待你的发言…"

    /**
     * 最近一轮的 AI 回复。AI 回复可能按句流式下发成多行，要拼出「本轮完整回复」，
     * 只取最后一行会丢掉前半句。
     */
    val latestAiText: String
        get() {
            val lastUserIndex = _messages.indexOfLast { it.fromMe }
            val reply = _messages.drop(lastUserIndex + 1)
                .filter { !it.fromMe }
                .joinToString("") { it.text }
            return reply.ifEmpty { "AI 正在聆听…" }
        }

    // --- 字幕合并状态：USER(ASR) 与 AGENT(LLM) 的拼接规则不同，各自独立维护 ---
    /** 用户本轮已稳定的识别文本。 */
    private var userStableText = ""
    /** 用户本轮尚未稳定、可被后续结果替换的文本。 */
    private var userPendingText = ""
    private var userRoundKey: String? = null
    /** AI 本轮「截至当前的完整回复文本」。 */
    private var agentText = ""
    /** 当前逻辑轮次 ID（不随换行加后缀，用于判断轮次是否变化）。 */
    private var agentRoundKey: String? = null
    /** 当前这一行在 _messages 里的 key，可能与 [agentRoundKey] 不同（带序号后缀）。 */
    private var agentLineKey: String? = null
    /** end=true 后本轮已结束，下一帧即使 roundId 相同也要另起一行。 */
    private var agentRoundClosed = false

    /** 本次会话的房间 / 用户 / 机器人标识，进房与创建任务必须共用同一套。 */
    private val roomId: String = AppConfig.DEFAULT_ROOM_ID
    private val localUserId: String = AppConfig.DEFAULT_USER_ID
    private val robotUserId: String =
        AppConfig.ROBOT_ID.ifBlank { localUserId + ROBOT_ID_SUFFIX }

    private val listener = object : AIConversationListener {
        override fun onConversationStarted(errorCode: Int, errorMessage: String) {
            if (errorCode != AIConversationError.CODE_OK) {
                Log.w(TAG, "进房失败，跳过创建 AI 任务：$errorCode $errorMessage")
                return
            }
            Log.i(TAG, "进房成功，开始创建云端 AI 对话任务")
            startAiConversationTask()
        }

        override fun onSubtitle(item: SubtitleItem) {
            if (!sessionStarted) {
                Log.w(TAG, "SUBTITLE ignored: 会话未启动, $item")
                return
            }
            val merged = if (item.role == SubtitleItem.Role.USER) {
                mergeUserAsr(item)
            } else {
                mergeAgentReply(item)
            }
            // 服务端在 VAD 起始、轮次收尾时会下发 text 为空的字幕帧。合并状态机
            // 照常更新（end / roundId 语义不能丢），但空帧既不新增行，也不能把
            // 已经显示的那一行清空，否则聊天列表会多出一条空白行。
            if (item.text.isBlank() || merged.text.isBlank()) {
                if (item.isEnd) {
                    // 空定稿帧同样要收敛上一行的流式光标。它常带着新 roundId，
                    // 这时按同角色最后一行兜底，否则光标会一直挂着。
                    val index = _messages.indexOfFirst { it.key == merged.key }
                        .takeIf { it >= 0 }
                        ?: _messages.indexOfLast { it.fromMe == merged.fromMe }
                    if (index >= 0 && !_messages[index].isFinal) {
                        _messages[index] = _messages[index].copy(isFinal = true)
                    }
                }
                Log.i(
                    TAG, "SUBTITLE skip-empty key=${merged.key} role=${item.role} " +
                        "isEnd=${item.isEnd} action=${merged.action} count=${_messages.size}"
                )
                return
            }
            val index = _messages.indexOfFirst { it.key == merged.key }
            val message = VoiceMessage(
                key = merged.key,
                fromMe = merged.fromMe,
                text = merged.text,
                isFinal = item.isEnd
            )
            if (index >= 0) _messages[index] = message else _messages.add(message)
            Log.i(
                TAG, "SUBTITLE key=${merged.key} role=${item.role} " +
                    "isEnd=${item.isEnd} taskId=${item.taskId} ts=${item.timestampMs} " +
                    "action=${merged.action} count=${_messages.size} " +
                    "delta=${item.text} merged=${merged.text}"
            )
        }

        override fun onAgentStateChanged(state: AgentState) {
            val previous = agentState
            if (!sessionStarted) {
                Log.w(TAG, "AGENT_STATE ignored: 会话未启动, $previous -> $state")
                return
            }
            agentState = state
            Log.i(TAG, "AGENT_STATE $previous -> $state")
        }

        override fun onConversationStopped() {
            Log.i(TAG, "会话结束")
        }

        override fun onError(error: AIConversationError) {
            Log.w(TAG, "AIConversationClient error: $error")
        }
    }

    /** 合并结果：定位用的 key、是否用户消息、最终展示文本、以及日志用的动作名。 */
    private data class SubtitleMerge(
        val key: String,
        val fromMe: Boolean,
        val text: String,
        val action: String
    )

    /**
     * 用户本段话（asrSubtitle）的合并，和 AI 回复的规则完全不同：
     *  - isEnd=false：本次识别尚不稳定，text 是「本段话截至当前的完整结果」，
     *    要替换掉上一次不稳定的结果（不是拼接）；
     *  - isEnd=true：本次识别已稳定，先用它替换掉之前不稳定的数据，
     *    再把它拼接到「上一个稳定片段」之后，成为新的稳定文本。
     * 展示文本 = 稳定文本 + 当前不稳定文本。同一轮次只占一行。
     */
    private fun mergeUserAsr(item: SubtitleItem): SubtitleMerge {
        val prefix = SubtitleItem.Role.USER.name + "|"
        val key = if (item.roundId.isNotBlank()) {
            prefix + item.roundId
        } else {
            userRoundKey ?: (prefix + item.timestampMs)
        }
        if (key != userRoundKey) {
            // 新一轮发言：清空上一轮的稳定/不稳定缓冲，另起一行
            userRoundKey = key
            userStableText = ""
            userPendingText = ""
            // 用户开口问问题时补一帧最新画面，让 AI 回答「你看这个」这类问题时
            // 有当下的画面可用；没开摄像头时 sendVideoSnapshot 会直接拒绝，无副作用。
            if (item.text.isNotBlank()) sendCameraSnapshot()
        }

        // 空文本帧只携带 end / roundId 语义，不能替换掉已经显示的识别结果。
        // 尤其是 end=true 的空定稿帧，原逻辑会清空 userPendingText 却没把它
        // 固化进 userStableText，整行文本会瞬间变空。
        if (item.text.isBlank()) {
            return SubtitleMerge(key, true, userStableText + userPendingText, "empty-keep")
        }

        val action: String
        if (item.isEnd) {
            // 稳定：替换掉不稳定数据，并接到上一个稳定片段之后。
            // 若服务端在稳定帧下发的是「含之前稳定内容的全量文本」，直接替换避免重复。
            if (userStableText.isNotEmpty() && item.text.startsWith(userStableText)) {
                userStableText = item.text
                action = "stable-replace-full"
            } else {
                userStableText += item.text
                action = "stable-append"
            }
            userPendingText = ""
        } else {
            // 不稳定：整体替换上一片不稳定结果
            userPendingText = item.text
            action = "unstable-replace"
        }
        return SubtitleMerge(key, true, userStableText + userPendingText, action)
    }

    /**
     * AI 回复（llmSubtitle）的合并，见文档 140338：
     * text 是「本轮回复截至当前的完整文本」，end=true 表示本轮回复结束。
     * SubtitleMode=1（句子级同步）时每句单独回调，文本不以旧文本开头，
     * 这时必须拼接才能保留前面已播报的句子；SubtitleMode=0 下发全量累积文本，
     * 以旧文本开头则直接替换。两种形态都要兼容。
     */
    private fun mergeAgentReply(item: SubtitleItem): SubtitleMerge {
        val prefix = SubtitleItem.Role.AGENT.name + "|"
        val roundId = if (item.roundId.isNotBlank()) {
            prefix + item.roundId
        } else {
            agentRoundKey ?: (prefix + item.timestampMs)
        }
        val startNewLine = roundId != agentRoundKey || agentRoundClosed || agentLineKey == null
        val key: String
        if (startNewLine) {
            // 同一 roundId 内另起一行时必须换 key，否则会覆盖上一行
            key = if (_messages.any { it.key.startsWith(roundId) }) {
                "$roundId|${item.timestampMs}"
            } else {
                roundId
            }
            agentRoundKey = roundId // 逻辑轮次不带后缀，避免下一帧被误判成新一轮
            agentLineKey = key
            agentText = ""
            agentRoundClosed = false
        } else {
            key = agentLineKey!!
        }
        // 空文本帧只更新轮次/结束状态，不改写本轮已累积的回复文本；
        // 否则 startNewLine 会为一条没有内容的字幕凭空创建一行。
        if (item.text.isBlank()) {
            if (item.isEnd) agentRoundClosed = true
            return SubtitleMerge(key, false, agentText, "empty-keep")
        }
        val action: String
        if (agentText.isEmpty()) {
            agentText = item.text
            action = "new-round"
        } else if (item.text.startsWith(agentText)) {
            agentText = item.text
            action = "replace-full"
        } else {
            agentText += item.text
            action = "append-fragment"
        }
        // 本轮回复结束。roundid 缺失时靠这个标志把下一帧分成新的一行，
        // 否则会一直复用同一个 fallback key、把多轮回复拼成一坨。
        if (item.isEnd) agentRoundClosed = true
        return SubtitleMerge(key, false, agentText, action)
    }

    /** 进入通话页时调用：设置监听并进房。重复调用不会重复进房。 */
    fun onPageEnter(context: Context) {
        if (sessionStarted) return
        val instance = AIConversationClient.getInstance(context.applicationContext)
        client = instance

        val startParams = AIConversationDef.StartParams(
            AppConfig.SDK_APP_ID,
            localUserId,
            GenerateTestUserSig.genTestUserSig(
                AppConfig.SDK_APP_ID, AppConfig.SDK_APP_KEY, localUserId
            ),
            roomId
        )
        instance.setListener(listener)
        val code = instance.start(startParams, robotUserId)
        if (code != AIConversationError.CODE_OK) {
            Log.w(TAG, "启动会话失败：$code")
            instance.setListener(null)
            client = null
            return
        }
        sessionStarted = true
    }

    /** 离开通话页时调用：停止云侧 AI 任务、注销监听、退房。幂等。 */
    fun onPageLeave() {
        if (!sessionStarted) return
        sessionStarted = false
        // 先停云侧任务再退房。若 start 的结果还没回来（taskId 为空），
        // 由 startAiConversationTask 的迟到回调补发 stop，避免任务残留计费。
        taskId?.let { stopAiConversationTask(it) }
        taskId = null
        client?.setListener(null)
        client?.stopLocalPreview()
        client?.stop()
        client = null
        _messages.clear()
        resetSubtitleMergeState()
        agentState = AgentState.IDLE
    }

    /** 清空两套字幕合并缓冲，避免下次会话沿用上一轮的 roundId 和累积文本。 */
    private fun resetSubtitleMergeState() {
        userStableText = ""
        userPendingText = ""
        userRoundKey = null
        agentText = ""
        agentRoundKey = null
        agentLineKey = null
        agentRoundClosed = false
    }

    /**
     * 开启本地摄像头预览（仅本地显示，不上行）。需要会话已启动且已取得 CAMERA 权限；
     * [view] 由界面创建并在预览期间保持挂载，ViewModel 不持有它。
     */
    fun startCameraPreview(view: TXCloudVideoView) {
        val current = client
        if (current == null) {
            Log.w(TAG, "startCameraPreview: 会话未启动，未开启预览")
            return
        }
        val code = current.startLocalPreview(view)
        if (code != AIConversationError.CODE_OK) {
            Log.w(TAG, "startCameraPreview: 返回 $code，预览未开启")
        }
    }

    /** 切换前后摄像头，仅预览开启时有效。 */
    fun switchCamera() {
        client?.switchCamera()
    }

    /**
     * 截取当前摄像头画面发给 AI（视觉理解），AI 会结合用户随后的语音提问理解画面。
     * 需要会话 READY 且预览已开启，否则只记日志。
     */
    fun sendCameraSnapshot() {
        if (!sessionStarted) return
        val current = client ?: return
        // 没开摄像头是常态（其他页面、或用户关了摄像头），静默跳过，不刷日志
        if (!current.isLocalPreviewing) return
        val code = current.sendVideoSnapshot()
        if (code != AIConversationError.CODE_OK) {
            Log.w(TAG, "sendCameraSnapshot: 返回 $code，画面未发送")
        }
    }

    /** 关闭本地摄像头预览，幂等。 */
    fun stopCameraPreview() {
        client?.stopLocalPreview()
    }

    /** 打断 AI 当前播报，供「说话或点击打断」使用。 */
    protected fun requestInterrupt() {
        if (!sessionStarted) return
        val code = client?.interrupt() ?: return
        if (code != AIConversationError.CODE_OK) {
            Log.w(TAG, "打断失败：$code")
        }
    }

    /** 点击麦克风按钮：切换 UI 状态并同步调用 AIConversationClient.muteLocalAudio。 */
    fun toggleMic() {
        micOn = !micOn
        val muted = !micOn // micOn=false 表示关闭麦克风，对应 SDK 的 muted=true
        val current = client
        if (current == null) {
            Log.w(TAG, "toggleMic: 会话未启动，未调用 muteLocalAudio($muted)")
            return
        }
        val code = current.muteLocalAudio(muted)
        if (code != AIConversationError.CODE_OK) {
            // 进房完成前调用会返回 CODE_ILLEGAL_STATE，此时 UI 与 SDK 静音状态不一致
            Log.w(TAG, "toggleMic: muteLocalAudio($muted) 返回 $code，静音未生效")
        } else {
            Log.i(TAG, "toggleMic: muteLocalAudio($muted) 已生效")
        }
    }

    /** 结束对话后恢复到初始状态，下次进入页面时从头开始。 */
    open fun reset() {
        micOn = true
    }

    /** 云 API 签名凭据与地域，取自 [AppConfig]。 */
    private fun cloudCredentials() = AIConversationRequest.Credentials(
        AppConfig.CLOUD_SECRET_ID, AppConfig.CLOUD_SECRET_KEY, AppConfig.CLOUD_REGION
    )

    /**
     * 进房成功后直连云 API 异步创建 AI 对话任务（CreateVoiceConversation）。
     */
    private fun startAiConversationTask() {
        val params = AIConversationRequest.RequestParams().apply {
            s2s = useS2S
            sdkAppId = AppConfig.SDK_APP_ID
            llmType = AppConfig.LLM_TYPE
            llmModel = AppConfig.LLM_MODEL
            llmApiUrl = AppConfig.LLM_API_URL
            llmApiKey = AppConfig.LLM_API_KEY
            roomId = this@AIConversationViewModel.roomId
            agentUserId = robotUserId
            agentUserSig = GenerateTestUserSig.genTestUserSig(
                AppConfig.SDK_APP_ID, AppConfig.SDK_APP_KEY, robotUserId
            )
            targetUserId = localUserId
            llmSystemPrompt = this@AIConversationViewModel.llmSystemPrompt
            ttsParams = this@AIConversationViewModel.ttsParams
            welcomeMessage = this@AIConversationViewModel.welcomeMessage
        }

        AIConversationRequest.start(cloudCredentials(), params,
            object : AIConversationRequest.Callback {
                override fun onSuccess(id: String?) {
                    if (id.isNullOrBlank()) {
                        Log.w(TAG, "AI 对话任务创建成功但未返回 TaskId")
                        return
                    }
                    // 请求是异步的，期间用户可能已结束对话并离开页面。
                    // 任务已在云端创建，必须补发 stop，否则会一直占用计费资源。
                    if (!sessionStarted) {
                        Log.w(TAG, "页面已离开，补发停止任务，TaskId=$id")
                        stopAiConversationTask(id)
                        return
                    }
                    taskId = id
                    Log.i(TAG, "AI 对话任务创建成功，TaskId=$id")
                }

                override fun onFailure(errorCode: Int, message: String?) {
                    Log.w(TAG, "AI 对话任务创建失败：[$errorCode] $message")
                }
            })
    }

    /** 直连云 API 异步停止 AI 对话任务；结果只记日志，页面已退出不再改 UI 状态。 */
    private fun stopAiConversationTask(id: String) {
        AIConversationRequest.stop(cloudCredentials(),
            AIConversationRequest.StopParams(AppConfig.SDK_APP_ID, id),
            object : AIConversationRequest.Callback {
                override fun onSuccess(stoppedId: String?) {
                    Log.i(TAG, "AI 对话任务已停止，TaskId=$id")
                }

                override fun onFailure(errorCode: Int, message: String?) {
                    Log.w(TAG, "AI 对话任务停止失败：[$errorCode] $message")
                }
            })
    }

    override fun onCleared() {
        onPageLeave()
        super.onCleared()
    }
}
