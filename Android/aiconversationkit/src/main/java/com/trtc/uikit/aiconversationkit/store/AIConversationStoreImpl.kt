package com.trtc.uikit.aiconversationkit.store

import android.Manifest
import android.content.pm.ApplicationInfo
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import com.tencent.liteav.device.TXDeviceManager.TXSystemVolumeType
import com.tencent.qcloud.tuicore.TUIConstants
import com.tencent.qcloud.tuicore.TUICore
import com.tencent.qcloud.tuicore.TUILogin
import com.tencent.qcloud.tuicore.permission.PermissionCallback
import com.tencent.qcloud.tuicore.permission.PermissionRequester
import com.tencent.trtc.TRTCCloud
import com.tencent.trtc.TRTCCloudDef
import com.tencent.trtc.TRTCCloudDef.TRTCAudioVolumeEvaluateParams
import com.tencent.trtc.TRTCCloudListener
import com.trtc.uikit.aiconversationkit.R
import com.trtc.uikit.aiconversationkit.common.Logger
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService
import com.trtc.uikit.aiconversationkit.manager.net.AIConversationRequest
import com.trtc.uikit.aiconversationkit.manager.net.ClientAIConversationRequest
import com.trtc.uikit.aiconversationkit.manager.net.ServerAIConversationRequest
import io.trtc.tuikit.atomicxcore.api.CompletionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.util.Locale

internal class AIConversationStoreImpl private constructor(): AIConversationStore() {

    private val _conversationMessageList = MutableStateFlow<List<ConversationMessage>>(emptyList())
    private val _aiStatus = MutableStateFlow<AIStatus>(AIStatus.OFFLINE)
    private val _isMicOpened = MutableStateFlow<Boolean>(false)

    private var _roomId = ""
    private var _interruptMode = 0
    private var _welcomeMessage = ""
    private val _localUserId = TUILogin.getUserId()
    private var _aiRobotUserId = ""

    private val context = TUILogin.getAppContext()
    private var aiConversationRequest: AIConversationRequest = ClientAIConversationRequest()

    companion object {
        val shared by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
            AIConversationStoreImpl()
        }
        private const val TAG: String = "AIConversationStore"
        private const val AI_MESSAGE_TYPE_TEXT: Int = 10000
        private const val AI_MESSAGE_TYPE_STATUS: Int = 10001
        private const val AI_MESSAGE_TYPE_LATENCY: Int = 10020
        private const val AI_MESSAGE_TYPE_ERROR: Int = 10030

        private const val INVALID_PARAMETER: Int = -1001
        private const val PERMISSION_DENIED: Int = -1003
    }

    override val conversationState = ConversationState(
        conversationMessageList = _conversationMessageList.asStateFlow(),
        aiStatus = _aiStatus.asStateFlow(),
        isMicOpened = _isMicOpened.asStateFlow()
    )

    init {
        TRTCCloud.sharedInstance(context).addListener(CloudListener())
        if (PackageService.isInternalDemo()) {
            aiConversationRequest = ServerAIConversationRequest()
        }
    }

    override fun startAIConversation(config: AIConversationConfig?, completion: CompletionHandler?) {
        if (config == null) {
            Logger.e(TAG, "startAIConversation config is null")
            completion?.onFailure(INVALID_PARAMETER, "config is null")
            return
        }
        _aiStatus.value = AIStatus.INITIALIZING
        val roomId = (10..99).random().toString() + TUILogin.getUserId()
        Logger.i(
            TAG, String.format(
                "startConversation sdkAppId:%s roomId:%s userId:%s aiRobotId:%s",
                TUILogin.getSdkAppId(), roomId, TUILogin.getUserId(), config.agentConfig.aiRobotId))
        requestMicPermission(object : CompletionHandler {
            override fun onSuccess() {
                enterRoom(roomId)
                aiConversationRequest.startConversation(roomId, config)
                openLocalAudio()
                _roomId = roomId
                _interruptMode = config.agentConfig.interruptMode
                _welcomeMessage = config.agentConfig.welcomeMessage
                completion?.onSuccess()
            }

            override fun onFailure(code: Int, desc: String) {
                _aiStatus.value = AIStatus.OFFLINE
                completion?.onFailure(code, desc)
            }
        })
    }

    override fun stopAIConversation(completion: CompletionHandler?) {
        Logger.i(TAG, "stopConversation")
        TRTCCloud.sharedInstance(context).stopLocalAudio()
        aiConversationRequest.stopConversation()
        TRTCCloud.sharedInstance(context).exitRoom()
        resetState()
    }

    override fun interruptSpeech() {
        try {
            val time = System.currentTimeMillis()
            val timeStamp = (time / 1000).toString()
            val payLoadContent = JSONObject()
            payLoadContent.put("timestamp", timeStamp)
            payLoadContent.put("id", TUILogin.getSdkAppId().toString() + "_" + _roomId)
            val interruptContent = JSONObject()
            interruptContent.put("type", 20001)
            interruptContent.put("sender", _localUserId)
            interruptContent.put("receiver", JSONArray(arrayOf<String>(_aiRobotUserId)))
            interruptContent.put("payload", payLoadContent)
            val interruptString = interruptContent.toString()
            val data: ByteArray? = interruptString.toByteArray(StandardCharsets.UTF_8)
            Logger.i(TAG, String.format("interruptConversation : %s", interruptString))
            TRTCCloud.sharedInstance(context).sendCustomCmdMsg(0x2, data, true, true)
        } catch (e: JSONException) {
            Logger.e(TAG, String.format("interruptConversation JSONException %s : ", e))
        }

        try {
            val params = JSONObject()
            params.put("pause", 0)
            params.put("maxCacheTimeInMs", 0)
            val jsonApi = JSONObject()
            jsonApi.put("api", "pauseRemoteAudioStream").put("params", params)
            TRTCCloud.sharedInstance(context).callExperimentalAPI(jsonApi.toString())
        } catch (e: JSONException) {
            Logger.e(TAG, String.format("clearPauseAudioBuffer JSONException : %s", e))
        }
    }

    override fun openLocalMicrophone(completion: CompletionHandler?) {
        TRTCCloud.sharedInstance(context).muteLocalAudio(false)
        _isMicOpened.value = true
        completion?.onSuccess()
    }

    override fun closeLocalMicrophone() {
        TRTCCloud.sharedInstance(context).muteLocalAudio(true)
        _isMicOpened.value = false
    }

    fun setAIRobotUserId(id: String?) {
        _aiRobotUserId = id ?: ""
    }

    internal fun resetState() {
        _conversationMessageList.value = emptyList()
        _aiStatus.value = AIStatus.OFFLINE
        _isMicOpened.value = false
    }

    private fun enterRoom(roomId: String) {
        val trtcParams = TRTCCloudDef.TRTCParams()
        trtcParams.sdkAppId = TUILogin.getSdkAppId()
        trtcParams.userId = TUILogin.getUserId()
        if (PackageService.isInternalDemo()) {
            trtcParams.roomId = roomId.toInt()
        } else {
            trtcParams.strRoomId = roomId
        }
        trtcParams.userSig = TUILogin.getUserSig()
        updateAudioConfig()
        TRTCCloud.sharedInstance(context).enterRoom(trtcParams, TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL)
    }

    private fun openLocalAudio() {
        val params = TRTCAudioVolumeEvaluateParams()
        params.enableSpectrumCalculation = true
        params.interval = 100
        val cloud = TRTCCloud.sharedInstance(context)
        cloud.enableAudioVolumeEvaluation(true, params)
        cloud.startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH)
        _isMicOpened.value = true
    }

    private fun updateAudioConfig() {
        val cloud = TRTCCloud.sharedInstance(context)
        cloud.callExperimentalAPI(
            "{\"api\":\"setFramework\",\"params\":"
                    + "{\"component\":25,\"framework\":1,\"language\":1}}"
        )
        cloud.callExperimentalAPI("{\"api\":\"enableAIDenoise\",\"params\":{\"enable\":true}}")
        // AI denoising version switched to the latest version
        cloud.callExperimentalAPI(
            "{\"api\":\"setPrivateConfig\",\"params\":{\"configs\":"
                    + "[{\"key\":\"Liteav.Audio.common.ans.version\",\"default\":\"0\",\"value\":\"4\"}]}}"
        )
        // Setting the AI denoising style
        cloud.callExperimentalAPI("{\"api\":\"setAudioAINSStyle\",\"params\":{\"style\":4}}")
        cloud.callExperimentalAPI("{\"api\":\"enableAudioAGC\",\"params\":{\"enable\":0}}")
        cloud.getDeviceManager().setSystemVolumeType(TXSystemVolumeType.TXSystemVolumeTypeMedia)
        // Send mute packets after setting muteAudio
        cloud.callExperimentalAPI("{\"api\":\"setLocalAudioMuteMode\",\"params\":{\"mode\":0}}")

        try {
            val jsonObject = JSONObject()
            jsonObject.put("api", "setPrivateConfig")
            val configArraysElement = JSONObject()
            configArraysElement.put("key", "Liteav.Audio.common.ains.near.field.threshold")
            configArraysElement.put("value", "0")
            configArraysElement.put("default", "50")
            // The threshold here can be set from 0 to 100, 0 is the weakest, 100 is the strongest, the default is 50,
            // the larger the value, the stronger the elimination of low-volume human voices.
            val configsArrays = JSONArray()
            configsArrays.put(configArraysElement)
            val configs = JSONObject()
            configs.put("configs", configsArrays)
            jsonObject.put("params", configs)
            cloud.callExperimentalAPI(jsonObject.toString())
        } catch (e: JSONException) {
            Logger.e(TAG, String.format("setAudioDenoiseStrength JSONException : %s", e.message))
        }
    }

    private fun requestMicPermission(completion: CompletionHandler) {
        val callback: PermissionCallback = object : PermissionCallback() {
            override fun onGranted() {
                completion.onSuccess()
            }

            override fun onDenied() {
                completion.onFailure(PERMISSION_DENIED, "mic permission denied")
            }
        }
        val title: String =
            context.getString(R.string.conversation_permission_mic_reason_title, getAppName())
        val description = (TUICore.createObject(
            TUIConstants.Privacy.PermissionsFactory.FACTORY_NAME,
            TUIConstants.Privacy.PermissionsFactory.PermissionsName.MICROPHONE_PERMISSIONS, null
        ) as? String)?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.conversation_permission_mic_reason)

        val tip = (TUICore.createObject(
            TUIConstants.Privacy.PermissionsFactory.FACTORY_NAME,
            TUIConstants.Privacy.PermissionsFactory.PermissionsName.MICROPHONE_PERMISSIONS_TIP, null
        ) as? String)?.takeIf { it.isNotEmpty() } ?: context.getString(R.string.conversation_tips_start_audio)

        PermissionRequester.newInstance(Manifest.permission.RECORD_AUDIO)
            .title(title)
            .description(description)
            .settingsTip(tip)
            .callback(callback)
            .request()
    }

    private fun getAppName(): String {
        val applicationInfo: ApplicationInfo = context.applicationInfo
        return context.packageManager.getApplicationLabel(applicationInfo).toString()
    }

    private inner class CloudListener: TRTCCloudListener() {
        override fun onRecvCustomCmdMsg(userId: String?, cmdID: Int, seq: Int, message: ByteArray?) {
            Log.i(TAG, "onRecvCustomCmdMsg: ${message?.let { String(it) }}")
            if (cmdID != 1 || message == null) {
                return
            }
            val data: JSONObject?
            val type: Int
            try {
                data = JSONObject(String(message))
                type = data.getInt("type")
            } catch (e: JSONException) {
                Logger.e(TAG, String.format("onRecvCustomCmdMsg JSONException : %s", e.message))
                return
            }

            if (AI_MESSAGE_TYPE_TEXT == type) {
                handleSpeechText(data)
                return
            }
            if (AI_MESSAGE_TYPE_STATUS == type) {
                handleAIStateData(data)
                return
            }
            if (AI_MESSAGE_TYPE_LATENCY == type) {
                Log.d(TAG, String.format("[aic-api] AI server latency:%s", data.toString()))
                return
            }
            if (AI_MESSAGE_TYPE_ERROR == type) {
                Logger.i(TAG, String.format("AI server error:%s", data.toString()))
                return
            }
        }

        override fun onEnterRoom(result: Long) {
            super.onEnterRoom(result)
            Logger.i(TAG, String.format(Locale.ROOT,"onEnterRoom result=%d", result))
        }

        override fun onRemoteUserEnterRoom(userId: String) {
            Logger.i(TAG, String.format("onRemoteUserEnterRoom userId=%s", userId))
        }

        override fun onRemoteUserLeaveRoom(userId: String, i: Int) {
            Logger.i(TAG, String.format("onRemoteUserLeaveRoom userId=%s", userId))
        }

        override fun onError(errCode: Int, errMsg: String?, extraInfo: Bundle?) {
            Logger.i(TAG, String.format(Locale.ROOT, "onError errCode=%d errMsg=%s", errCode, errMsg))
        }

        override fun onExitRoom(reason: Int) {
            Logger.i(TAG, String.format(Locale.ROOT,"onExitRoom reason=%d", reason))
            if (reason == 0) {
                return
            }
            resetState()
        }

        private fun handleAIStateData(data: JSONObject) {
            var state = -1
            try {
                val sender = data.getString("sender")
                if (TextUtils.equals(sender, _localUserId)) {
                    return
                }
                val payload = data.getJSONObject("payload")
                state = payload.getInt("state")
            } catch (e: JSONException) {
                Logger.e(TAG, String.format("handleAIStateData JSONException : %s", e.message))
            }
            AIStatus.values().find { it.value == state }?.let {
                _aiStatus.value = it
            }
        }

        private fun handleSpeechText(data: JSONObject) {
            val message = ConversationMessage()
            try {
                val sender = data.getString("sender")
                val payload = data.getJSONObject("payload")
                message.roundId = payload.getString("roundid") ?: ""
                val text = payload.getString("text") ?: ""
                if (sender == _localUserId) {
                    message.userSpeechText = text
                } else {
                    message.aiSpeechText = text
                }
                message.isCompleted = if (text.isEmpty()) payload.getBoolean("end") else false
            } catch (e: JSONException) {
                Logger.e(TAG, String.format("handleSpeechText JSONException=%s data=%s", e.message, data.toString()))
            }

            val currentList = _conversationMessageList.value.toMutableList()
            val existingIndex = currentList.indexOfFirst { it.roundId == message.roundId }
            if (existingIndex >= 0) {
                val existingMessage = currentList[existingIndex]
                val updatedMessage = existingMessage.copy(
                    userSpeechText = if (message.userSpeechText.isNotEmpty()) message.userSpeechText else existingMessage.userSpeechText,
                    aiSpeechText = existingMessage.aiSpeechText + message.aiSpeechText,
                    isCompleted = if (message.aiSpeechText.isEmpty()) message.isCompleted else existingMessage.isCompleted
                )
                currentList[existingIndex] = updatedMessage
            } else {
                message.timestamp = System.currentTimeMillis()
                currentList.add(message)
            }
            _conversationMessageList.value = currentList.toList()
        }
    }
}