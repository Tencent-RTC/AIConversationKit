package com.trtc.uikit.aiconversationkit.manager;

import static com.tencent.liteav.device.TXDeviceManager.TXSystemVolumeType.TXSystemVolumeTypeMedia;
import static com.tencent.liteav.device.TXDeviceManager.TXSystemVolumeType.TXSystemVolumeTypeVOIP;
import static com.tencent.trtc.TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH;
import static com.tencent.trtc.TRTCCloudDef.TRTC_AUDIO_ROUTE_EARPIECE;
import static com.tencent.trtc.TRTCCloudDef.TRTC_AUDIO_ROUTE_SPEAKER;
import static com.trtc.uikit.aiconversationkit.view.ConversationConstant.DENOISES;
import static com.trtc.uikit.aiconversationkit.view.ConversationConstant.INTERRUPT_MODE_SMART;

import android.Manifest;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import android.util.Log;

import com.tencent.qcloud.tuicore.ServiceInitializer;
import com.tencent.qcloud.tuicore.TUIConstants;
import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.tencent.qcloud.tuicore.permission.PermissionCallback;
import com.tencent.qcloud.tuicore.permission.PermissionRequester;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.trtc.uikit.aiconversationkit.AIConversationDefine;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;
import com.trtc.uikit.aiconversationkit.manager.net.AIConversationRequest;
import com.trtc.uikit.aiconversationkit.manager.net.ClientAIConversationRequest;
import com.trtc.uikit.aiconversationkit.manager.net.ClientFeedbackRequest;
import com.trtc.uikit.aiconversationkit.manager.net.FeedbackRequest;
import com.trtc.uikit.aiconversationkit.manager.net.ServerAIConversationRequest;
import com.trtc.uikit.aiconversationkit.manager.net.ServerFeedbackRequest;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class ConversationManager {
    private static final    String              TAG         = "ConversationManager";
    private static final    int                 ROOM_ID_LEN = 9;
    private static volatile ConversationManager sInstance;

    private final Context               mAppContext            = ServiceInitializer.getAppContext();
    private final ConversationState     mConversationState     = new ConversationState();
    private final TRTCCloud             mTRTCCloud             = TRTCCloud.sharedInstance(mAppContext);
    private final TRTCCloudObserver     mTRTCCloudObserver     = new TRTCCloudObserver(mConversationState);
    private       AIConversationRequest mAIConversationRequest = new ClientAIConversationRequest();
    private       FeedbackRequest       mFeedbackRequest       = new ClientFeedbackRequest();

    private ConversationManager() {
        if (PackageService.isInternalDemo()) {
            mAIConversationRequest = new ServerAIConversationRequest();
            mFeedbackRequest = new ServerFeedbackRequest();
        }
        mTRTCCloud.addListener(mTRTCCloudObserver);
        mTRTCCloud.setAudioFrameListener(mTRTCCloudObserver);
    }

    public static ConversationManager sharedInstance() {
        if (sInstance == null) {
            synchronized (ConversationManager.class) {
                if (sInstance == null) {
                    sInstance = new ConversationManager();
                }
            }
        }
        return sInstance;
    }

    public void destroySharedInstance() {
        sInstance = null;
        mTRTCCloud.removeListener(mTRTCCloudObserver);
        mTRTCCloud.setAudioFrameListener(null);
    }

    public ConversationState getConversationState() {
        return mConversationState;
    }

    public void startConversation(AIConversationDefine.StartAIConversationParams params) {
        Log.d(TAG, "startConversation");
        if (TextUtils.isEmpty(params.roomId)) {
            params.roomId = TUILogin.getUserId() + System.currentTimeMillis();
        }
        enterRoom(params);
        mAIConversationRequest.startConversation(params);
        openLocalAudio();
        mConversationState.strRoomId = params.roomId;
        mConversationState.interruptMode.set(params.agentConfig.interruptMode);
        mConversationState.welcomeMessage.set(params.agentConfig.welcomeMessage);
        mFeedbackRequest.fetchRemainingExperienceTime();
        mFeedbackRequest.fetchFeedback();
    }

    public void stopConversation() {
        Log.d(TAG, "stopConversation");
        mTRTCCloud.stopLocalAudio();
        mAIConversationRequest.stopConversation();
        mTRTCCloud.exitRoom();
        destroySharedInstance();
    }

    public void deductionExperienceTime() {
        mFeedbackRequest.deductionExperienceTime();
    }

    public void interruptConversation() {
        if (mConversationState.interruptMode.get() == INTERRUPT_MODE_SMART) {
            return;
        }
        try {
            long time = System.currentTimeMillis();
            String timeStamp = String.valueOf(time / 1000);
            JSONObject payLoadContent = new JSONObject();
            payLoadContent.put("timestamp", timeStamp);
            payLoadContent.put("id", TUILogin.getSdkAppId() + "_" + mConversationState.strRoomId);
            JSONObject interruptContent = new JSONObject();
            interruptContent.put("type", 20001);
            interruptContent.put("sender", mConversationState.localUserId);
            interruptContent.put("receiver", new JSONArray(new String[]{mConversationState.aiRobotUserId}));
            interruptContent.put("payload", payLoadContent);
            String interruptString = interruptContent.toString();
            byte[] data = interruptString.getBytes(StandardCharsets.UTF_8);
            Log.d(TAG, String.format("interruptConversation : %s", interruptString));
            mTRTCCloud.sendCustomCmdMsg(0x2, data, true, true);
            mConversationState.isPaused.set(false);
            clearPauseAudioBuffer();
        } catch (JSONException e) {
            Log.e(TAG, String.format("interruptConversation JSONException %s : ", e));
        }
    }

    private void clearPauseAudioBuffer() {
        try {
            JSONObject params = new JSONObject();
            params.put("pause", 0);
            params.put("maxCacheTimeInMs", 0);
            JSONObject jsonApi = new JSONObject();
            jsonApi.put("api", "pauseRemoteAudioStream").put("params", params);
            mTRTCCloud.callExperimentalAPI(jsonApi.toString());
        } catch (JSONException e) {
            Log.e(TAG, String.format("clearPauseAudioBuffer JSONException : %s", e));
        }

    }

    public void uploadFeedback(Map<String, String> feedback, TUIServiceCallback callback) {
        mFeedbackRequest.uploadFeedback(feedback, callback);
    }

    public void toggleLocalAudio() {
        if (!mConversationState.isAudioMuted.get()) {
            Log.d(TAG, "muteLocalAudio true");
            mTRTCCloud.muteLocalAudio(true);
            mConversationState.isAudioMuted.set(true);
            return;
        }
        if (!mConversationState.isAudioOpened.get()) {
            openLocalAudio();
            return;
        }
        Log.d(TAG, "muteLocalAudio false");
        mTRTCCloud.muteLocalAudio(false);
        mConversationState.isAudioMuted.set(false);
    }

    public void toggleSpeaker() {
        boolean isSpeaker = !mConversationState.isSpeakerOpened.get();
        mTRTCCloud.getDeviceManager().setSystemVolumeType(isSpeaker ? TXSystemVolumeTypeMedia : TXSystemVolumeTypeVOIP);
        mTRTCCloud.setAudioRoute(isSpeaker ? TRTC_AUDIO_ROUTE_SPEAKER : TRTC_AUDIO_ROUTE_EARPIECE);
        mConversationState.isSpeakerOpened.set(isSpeaker);
    }

    public void toggleAIAudio() {
        boolean isPause = !mConversationState.isPaused.get();
        JSONObject jsonApi = new JSONObject();
        try {
            JSONObject params = new JSONObject();
            params.put("pause", isPause ? 1 : 0);
            params.put("maxCacheTimeInMs", 6000 * 60);
            jsonApi.put("api", "pauseRemoteAudioStream").put("params", params);
        } catch (JSONException e) {
            return;
        }
        mTRTCCloud.callExperimentalAPI(jsonApi.toString());
        mConversationState.isPaused.set(isPause);
    }

    private void openLocalAudio() {
        PermissionCallback callback = new PermissionCallback() {
            @Override
            public void onGranted() {
                Log.d(TAG, "openLocalAudio");
                TRTCCloudDef.TRTCAudioVolumeEvaluateParams params = new TRTCCloudDef.TRTCAudioVolumeEvaluateParams();
                params.enableSpectrumCalculation = true;
                params.interval = 100;
                mTRTCCloud.enableAudioVolumeEvaluation(true, params);
                mTRTCCloud.startLocalAudio(TRTC_AUDIO_QUALITY_SPEECH);
                mConversationState.isAudioOpened.set(true);
                mConversationState.isAudioMuted.set(false);
            }

            @Override
            public void onDenied() {
                Log.d(TAG, "openLocalAudio permission denied");
            }
        };
        String title =
                mAppContext.getString(R.string.conversation_permission_mic_reason_title, getAppName());
        String description = (String) TUICore.createObject(TUIConstants.Privacy.PermissionsFactory.FACTORY_NAME,
                TUIConstants.Privacy.PermissionsFactory.PermissionsName.MICROPHONE_PERMISSIONS, null);
        description = TextUtils.isEmpty(description) ? mAppContext.getString(
                R.string.conversation_permission_mic_reason) : description;

        String tip = (String) TUICore.createObject(TUIConstants.Privacy.PermissionsFactory.FACTORY_NAME,
                TUIConstants.Privacy.PermissionsFactory.PermissionsName.MICROPHONE_PERMISSIONS_TIP, null);
        tip = TextUtils.isEmpty(tip) ? mAppContext.getString(R.string.conversation_tips_start_audio) : tip;

        PermissionRequester.newInstance(Manifest.permission.RECORD_AUDIO)
                .title(title)
                .description(description)
                .settingsTip(tip)
                .callback(callback)
                .request();
    }

    private void enterRoom(AIConversationDefine.StartAIConversationParams params) {
        TRTCCloudDef.TRTCParams trtcParams = new TRTCCloudDef.TRTCParams();
        trtcParams.sdkAppId = TUILogin.getSdkAppId();
        trtcParams.userId = TUILogin.getUserId();
        if (PackageService.isInternalDemo()) {
            trtcParams.roomId = Integer.parseInt(params.roomId);
        } else {
            trtcParams.strRoomId = params.roomId;
        }
        trtcParams.userSig = TUILogin.getUserSig();
        updateAudioConfig(DENOISES[params.denoise.getValue()]);
        mTRTCCloud.enterRoom(trtcParams, TRTCCloudDef.TRTC_APP_SCENE_AUDIOCALL);
    }

    private void updateAudioConfig(int denoiseStrength) {
        mTRTCCloud.callExperimentalAPI("{\"api\":\"setFramework\",\"params\":"
                + "{\"component\":25,\"framework\":1,\"language\":1}}");
        mTRTCCloud.callExperimentalAPI("{\"api\":\"enableAIDenoise\",\"params\":{\"enable\":true}}");
        // AI denoising version switched to the latest version
        mTRTCCloud.callExperimentalAPI("{\"api\":\"setPrivateConfig\",\"params\":{\"configs\":"
                + "[{\"key\":\"Liteav.Audio.common.ans.version\",\"default\":\"0\",\"value\":\"4\"}]}}");
        // Setting the AI denoising style
        mTRTCCloud.callExperimentalAPI("{\"api\":\"setAudioAINSStyle\",\"params\":{\"style\":4}}");
        mTRTCCloud.callExperimentalAPI("{\"api\":\"enableAudioAGC\",\"params\":{\"enable\":0}}");
        mTRTCCloud.getDeviceManager().setSystemVolumeType(TXSystemVolumeTypeMedia);
        // Send mute packets after setting muteAudio
        mTRTCCloud.callExperimentalAPI("{\"api\":\"setLocalAudioMuteMode\",\"params\":{\"mode\":0}}");

        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("api", "setPrivateConfig");
            JSONObject configArraysElement = new JSONObject();
            configArraysElement.put("key", "Liteav.Audio.common.ains.near.field.threshold");
            configArraysElement.put("value", Integer.toString(denoiseStrength));
            configArraysElement.put("default", "50");
            // The threshold here can be set from 0 to 100, 0 is the weakest, 100 is the strongest, the default is 50,
            // the larger the value, the stronger the elimination of low-volume human voices.
            JSONArray configsArrays = new JSONArray();
            configsArrays.put(configArraysElement);
            JSONObject configs = new JSONObject();
            configs.put("configs", configsArrays);
            jsonObject.put("params", configs);
            mTRTCCloud.callExperimentalAPI(jsonObject.toString());
        } catch (JSONException e) {
            Log.e(TAG, String.format("setAudioDenoiseStrength JSONException : %s", e.getMessage()));
        }
    }

    private String generateRandomRoomId(int numberOfDigits) {
        if (numberOfDigits > 9) {
            numberOfDigits = 9;
        }
        Random random = new Random();
        int minNumber = (int) Math.pow(10, numberOfDigits - 1);
        int maxNumber = (int) Math.pow(10, numberOfDigits) - 1;
        int randomNumber = random.nextInt(maxNumber - minNumber) + minNumber;
        Log.d(TAG, "generateRandomRoomId : " + randomNumber);
        return String.valueOf(randomNumber);
    }

    private String getAppName() {
        ApplicationInfo applicationInfo = mAppContext.getApplicationInfo();
        return mAppContext.getPackageManager().getApplicationLabel(applicationInfo).toString();
    }
}
