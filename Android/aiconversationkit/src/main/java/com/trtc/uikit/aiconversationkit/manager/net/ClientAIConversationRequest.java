package com.trtc.uikit.aiconversationkit.manager.net;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.tencent.qcloud.tuicore.TUILogin;
import com.trtc.uikit.aiconversationkit.AIConversationDefine;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;

public class ClientAIConversationRequest implements AIConversationRequest {
    private static final String TAG                          = "ClientAIConversationReq";
    private static final String VERSION                      = "2019-07-22";
    private static final String ACTION_START_AI_CONVERSATION = "StartAIConversation";
    private static final String ACTION_STOP_AI_CONVERSATION  = "StopAIConversation";

    private final OkHttpClient mClient    = new OkHttpClient();
    private final Gson         mGson      = new Gson();
    private       String       mSecretId  = "";
    private       String       mSecretKey = "";
    private       String       mRegion    = "";

    @Override
    public void startConversation(AIConversationDefine.StartAIConversationParams params) {
        mSecretId = params.secretId;
        mSecretKey = params.secretKey;
        mRegion = params.region;
        String body = generateStartConversationBody(params);
        Log.i(TAG, String.format("startConversation roomId=%s userId=%s", params.roomId, TUILogin.getUserId()));

        Runnable startAIConversationRun = () -> {
            Request request = buildRequest(mSecretId, mSecretKey, VERSION, ACTION_START_AI_CONVERSATION, body,
                    mRegion, "");
            String response = "";
            try {
                ResponseBody responseBody = mClient.newCall(request).execute().body();
                response = responseBody.string();
            } catch (IOException e) {
                Log.e(TAG, "startConversation response IOException : " + e.getMessage());
            } catch (NullPointerException ne) {
                Log.e(TAG, "startConversation response NullPointerException : " + ne.getMessage());
            }
            Log.i(TAG, "startAIConversation response = " + response);
            if (TextUtils.isEmpty(response)) {
                return;
            }
            ConversationState state = ConversationManager.sharedInstance().getConversationState();
            state.aiRobotUserId = params.agentConfig.aiRobotId;
            try {
                JSONObject jsonObject = new JSONObject(response);
                state.conversationTaskId = jsonObject.getJSONObject("Response").getString("TaskId");
            } catch (JSONException e) {
                Log.e(TAG, "conversationTaskId JSONException : " + e.getMessage());
            }
        };
        new Thread(startAIConversationRun).start();
    }

    @Override
    public void stopConversation() {
        String body = generateStopConversationBody();
        Log.i(TAG, "stopConversation : " + body);

        Runnable stopAIConversationRun = () -> {
            Request request = buildRequest(mSecretId, mSecretKey, VERSION, ACTION_STOP_AI_CONVERSATION, body,
                    mRegion, "");
            String response = "";
            try {
                ResponseBody responseBody = mClient.newCall(request).execute().body();
                response = responseBody.string();
            } catch (IOException e) {
                Log.e(TAG, "startConversation response IOException : " + e.getMessage());
            } catch (NullPointerException ne) {
                Log.e(TAG, "startConversation response NullPointerException : " + ne.getMessage());
            }
            Log.i(TAG, "stopConversation response = " + response);
        };
        new Thread(stopAIConversationRun).start();
    }

    private String generateStartConversationBody(AIConversationDefine.StartAIConversationParams params) {
        HashMap<String, Object> agentMap = new HashMap<>();
        agentMap.put("UserId", params.agentConfig.aiRobotId);
        agentMap.put("UserSig", params.agentConfig.aiRobotSig);
        agentMap.put("TargetUserId", TUILogin.getUserId());
        if (!TextUtils.isEmpty(params.agentConfig.welcomeMessage)) {
            agentMap.put("WelcomeMessage", params.agentConfig.welcomeMessage);
        }
        agentMap.put("MaxIdleTime", params.agentConfig.maxIdleTime);
        agentMap.put("InterruptMode", params.agentConfig.interruptMode);
        agentMap.put("InterruptSpeechDuration", params.agentConfig.interruptSpeechDuration);
        agentMap.put("TurnDetectionMode", params.agentConfig.turnDetectionMode);
        agentMap.put("WelcomeMessagePriority", params.agentConfig.welcomeMessagePriority);
        agentMap.put("FilterOneWord", params.agentConfig.filterOneWord);

        HashMap<String, Object> sttMap = new HashMap<>();
        if (!TextUtils.isEmpty(params.sttConfig.language)) {
            sttMap.put("Language", params.sttConfig.language);
        }
        if (params.sttConfig.alternativeLanguage != null && !params.sttConfig.alternativeLanguage.isEmpty()) {
            sttMap.put("AlternativeLanguage", params.sttConfig.alternativeLanguage);
        }
        if (!TextUtils.isEmpty(params.sttConfig.customParam)) {
            sttMap.put("Language", params.sttConfig.customParam);
        }
        if (!TextUtils.isEmpty(params.sttConfig.hotWordList)) {
            sttMap.put("hotWordList", params.sttConfig.hotWordList);
        }

        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("SdkAppId", TUILogin.getSdkAppId());
        dataMap.put("RoomId", params.roomId);
        dataMap.put("RoomIdType", ConversationConstant.ROOM_TYPE_STRING);
        dataMap.put("AgentConfig", agentMap);
        dataMap.put("STTConfig", sttMap);
        dataMap.put("LLMConfig", params.llmConfig);
        dataMap.put("TTSConfig", params.ttsConfig);
        return mGson.toJson(dataMap);
    }

    private String generateStopConversationBody() {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("TaskId", ConversationManager.sharedInstance().getConversationState().conversationTaskId);
        return mGson.toJson(dataMap);
    }

    private Request buildRequest(String secretId,
                                 String secretKey,
                                 String version,
                                 String action,
                                 String body,
                                 String region,
                                 String token) {
        String host = "trtc.tencentcloudapi.com";
        String url = "https://" + host;
        String contentType = "application/json; charset=utf-8";
        String timestamp = java.lang.String.valueOf(System.currentTimeMillis() / 1000);
        String auth = "";
        try {
            auth = getAuth(secretId, secretKey, host, contentType, timestamp, body);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            Log.e(TAG, String.format("getAuth Exception : %s", e.getMessage()));
        }
        return new Request.Builder()
                .header("Host", host)
                .header("X-TC-Timestamp", timestamp)
                .header("X-TC-Version", version)
                .header("X-TC-Action", action)
                .header("X-TC-Region", region)
                .header("X-TC-Token", token)
                .header("X-TC-RequestClient", "SDK_JAVA_BAREBONE")
                .header("Authorization", auth)
                .url(url)
                .post(RequestBody.create(body, MediaType.get(contentType)))
                .build();
    }

    private String getAuth(String secretId,
                           String secretKey,
                           String host,
                           String contentType,
                           String timestamp,
                           String body) throws NoSuchAlgorithmException, InvalidKeyException {
        String canonicalUri = "/";
        String canonicalQueryString = "";
        String canonicalHeaders = "content-type:" + contentType + "\nhost:" + host + "\n";
        String signedHeaders = "content-type;host";

        String hashedRequestPayload = sha256Hex(body.getBytes(StandardCharsets.UTF_8));
        String canonicalRequest = "POST" + "\n" + canonicalUri + "\n" + canonicalQueryString + "\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + hashedRequestPayload;

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        String date = sdf.format(new Date(Long.valueOf(timestamp + "000")));
        String service = host.split("\\.")[0];
        String credentialScope = date + "/" + service + "/" + "tc3_request";
        String hashedCanonicalRequest = sha256Hex(canonicalRequest.getBytes(StandardCharsets.UTF_8));
        String stringToSign = "TC3-HMAC-SHA256\n" + timestamp + "\n" + credentialScope + "\n" + hashedCanonicalRequest;
        byte[] secretDate = hmac256(("TC3" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] secretService = hmac256(secretDate, service);
        byte[] secretSigning = hmac256(secretService, "tc3_request");
        String signature = printHexBinary(hmac256(secretSigning, stringToSign)).toLowerCase();
        return "TC3-HMAC-SHA256 " + "Credential=" + secretId + "/" + credentialScope + ", "
                + "SignedHeaders=" + signedHeaders + ", " + "Signature=" + signature;
    }

    private String sha256Hex(byte[] b) throws NoSuchAlgorithmException {
        MessageDigest md;
        md = MessageDigest.getInstance("SHA-256");
        byte[] d = md.digest(b);
        return printHexBinary(d).toLowerCase();
    }

    private byte[] hmac256(byte[] key, String msg) throws NoSuchAlgorithmException, InvalidKeyException {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(key, mac.getAlgorithm());
        mac.init(secretKeySpec);
        return mac.doFinal(msg.getBytes(StandardCharsets.UTF_8));
    }

    private String printHexBinary(byte[] data) {
        StringBuilder r = new StringBuilder(data.length * 2);
        char[] hexCode = "0123456789ABCDEF".toCharArray();
        for (byte b : data) {
            r.append(hexCode[(b >> 4) & 0xF]);
            r.append(hexCode[(b & 0xF)]);
        }
        return r.toString();
    }
}
