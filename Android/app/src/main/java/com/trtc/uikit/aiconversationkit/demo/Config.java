package com.trtc.uikit.aiconversationkit.demo;

import android.text.TextUtils;
import android.util.Log;

import com.trtc.uikit.aiconversationkit.AIConversationDefine;

import org.json.JSONException;
import org.json.JSONObject;

public class Config {

    public static final String SECRET_ID  = "";
    public static final String SECRET_KEY = "";
    public static final String CONFIG     = "";

    private static final String TAG        = "Config";

    public static AIConversationDefine.StartAIConversationParams parseStartAIConversationParams(String secretId,
                                                                                                String secretKey,
                                                                                                String config) {
        AIConversationDefine.StartAIConversationParams params = new AIConversationDefine.StartAIConversationParams();
        params.secretId = secretId;
        params.secretKey = secretKey;

        try {
            JSONObject configObject = new JSONObject(config);
            JSONObject chatObject = configObject.getJSONObject("chatConfig");

            JSONObject agentObject = chatObject.getJSONObject("AgentConfig");
            params.agentConfig = new AIConversationDefine.AgentConfig();
            params.agentConfig.aiRobotId = agentObject.getString("UserId");
            params.agentConfig.aiRobotSig = agentObject.getString("UserSig");
            if (agentObject.has("WelcomeMessage")) {
                params.agentConfig.welcomeMessage = agentObject.getString("WelcomeMessage");
            }
            if (agentObject.has("InterruptMode")) {
                params.agentConfig.interruptMode = agentObject.getInt("InterruptMode");
            }
            if (agentObject.has("InterruptSpeechDuration")) {
                params.agentConfig.interruptSpeechDuration = agentObject.getInt("InterruptSpeechDuration");
            }

            JSONObject sttObject = chatObject.getJSONObject("STTConfig");
            if (sttObject.has("Language")) {
                params.sttConfig.language = sttObject.getString("Language");
            }
            if (sttObject.has("VadSilenceTime")) {
                params.sttConfig.vadSilenceTime = sttObject.getInt("VadSilenceTime");
            }

            params.llmConfig = chatObject.getString("LLMConfig");
            params.ttsConfig = chatObject.getString("TTSConfig");
        } catch (JSONException | NullPointerException e) {
            Log.e(TAG, String.format("parseStartAIConversationParams Exception : %s", e.getMessage()));
        }
        return params;
    }

    public static boolean isConfigAvailable() {
        Log.i(TAG, String.format("config = %s", CONFIG));
        return !TextUtils.isEmpty(CONFIG) && !TextUtils.isEmpty(SECRET_ID) && !TextUtils.isEmpty(SECRET_KEY);
    }

    public static int getSdkAppId() {
        int sdkAppId = 0;
        try {
            JSONObject jsonObject = new JSONObject(CONFIG);
            JSONObject userInfo = jsonObject.getJSONObject("userInfo");
            sdkAppId = userInfo.getInt("sdkAppId");
        } catch (JSONException e) {
            Log.e(TAG, String.format("getSdkAppId JSONException : %s", e.getMessage()));
        }
        return sdkAppId;
    }

    public static String getUserId() {
        String userId = "";
        try {
            JSONObject jsonObject = new JSONObject(CONFIG);
            JSONObject userInfo = jsonObject.getJSONObject("userInfo");
            userId = userInfo.getString("userId");
        } catch (JSONException e) {
            Log.e(TAG, String.format("getUserId JSONException : %s", e.getMessage()));
        }
        return userId;
    }

    public static String getUserSig() {
        String userSig = "";
        try {
            JSONObject jsonObject = new JSONObject(CONFIG);
            JSONObject userInfo = jsonObject.getJSONObject("userInfo");
            userSig = userInfo.getString("userSig");
        } catch (JSONException e) {
            Log.e(TAG, String.format("getUserSig JSONException : %s", e.getMessage()));
        }
        return userSig;
    }
}
