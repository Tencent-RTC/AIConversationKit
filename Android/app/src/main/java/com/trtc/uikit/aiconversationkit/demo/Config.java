package com.trtc.uikit.aiconversationkit.demo;

import android.text.TextUtils;
import android.util.Log;

import com.trtc.uikit.aiconversationkit.store.AIConversationConfig;

import org.json.JSONException;
import org.json.JSONObject;

public class Config {
    private static final String TAG        = "AIConfig";

    public static final String SECRET_ID  = "";
    public static final String SECRET_KEY = "";
    public static final String CONFIG     = "";

    public static AIConversationConfig parseAIConversationConfig(String secretId, String secretKey, String config) {
        AIConversationConfig aiConfig = new AIConversationConfig();
        aiConfig.setSecretId(secretId);
        aiConfig.setSecretKey(secretKey);

        try {
            JSONObject configObject = new JSONObject(config);
            JSONObject chatObject = configObject.getJSONObject("chatConfig");

            JSONObject agentObject = chatObject.getJSONObject("AgentConfig");
            aiConfig.getAgentConfig().setAiRobotId(agentObject.getString("UserId"));
            aiConfig.getAgentConfig().setAiRobotSig(agentObject.getString("UserSig"));
            if (agentObject.has("WelcomeMessage")) {
                aiConfig.getAgentConfig().setWelcomeMessage(agentObject.getString("WelcomeMessage"));
            }
            if (agentObject.has("InterruptMode")) {
                aiConfig.getAgentConfig().setInterruptMode(agentObject.getInt("InterruptMode"));
            }

            JSONObject sttObject = chatObject.getJSONObject("STTConfig");
            if (sttObject.has("Language")) {
                aiConfig.getSttConfig().setLanguage(sttObject.getString("Language"));
            }
            if (sttObject.has("VadSilenceTime")) {
                aiConfig.getSttConfig().setVadSilenceTime(sttObject.getInt("VadSilenceTime"));
            }

            aiConfig.setLlmConfig(chatObject.getString("LLMConfig"));
            aiConfig.setTtsConfig(chatObject.getString("TTSConfig"));
            if (configObject.has("region")) {
                aiConfig.setRegion(configObject.getString("region"));
            }
        } catch (JSONException | NullPointerException e) {
            Log.e(TAG, String.format("parseAIConversationConfig Exception : %s", e.getMessage()));
        }
        return aiConfig;
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
