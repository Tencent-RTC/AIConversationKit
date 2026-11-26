package com.trtc.uikit.aiconversationkit.manager.net;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.tencent.qcloud.tuicore.TUILogin;
import com.trtc.uikit.aiconversationkit.store.AIConversationConfig;
import com.trtc.uikit.aiconversationkit.common.Logger;
import com.trtc.uikit.aiconversationkit.view.conversation.ConversationConstant;

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
    private volatile String    mTaskId    = "";

    @Override
    public void startConversation(String roomId, AIConversationConfig config) {
        mSecretId = config.getSecretId();
        mSecretKey = config.getSecretKey();
        mRegion = config.getRegion();
        String body = generateStartConversationBody(roomId, config);
        Logger.i(TAG, String.format("startConversation roomId:%s userId:%s aiRobotId:%s", roomId,
                TUILogin.getUserId(), config.getAgentConfig().getAiRobotId()));

        Runnable startAIConversationRun = () -> {
            Request request = buildRequest(mSecretId, mSecretKey, VERSION, ACTION_START_AI_CONVERSATION, body,
                    mRegion, "");
            String response = "";
            try {
                ResponseBody responseBody = mClient.newCall(request).execute().body();
                response = responseBody.string();
            } catch (IOException e) {
                Logger.e(TAG, String.format("startConversation response IOException : %s", e.getMessage()));
            } catch (NullPointerException ne) {
                Logger.e(TAG, String.format("startConversation response NullPointerException : %s", ne.getMessage()));
            }
            Logger.i(TAG, String.format("startAIConversation response : %s", response));
            if (TextUtils.isEmpty(response)) {
                return;
            }
            try {
                JSONObject jsonObject = new JSONObject(response);
                mTaskId = jsonObject.getJSONObject("Response").getString("TaskId");
            } catch (JSONException e) {
                Logger.e(TAG, String.format("conversationTaskId JSONException : %s", e.getMessage()));
            }
        };
        new Thread(startAIConversationRun).start();
    }

    @Override
    public void stopConversation() {
        String body = generateStopConversationBody();
        Logger.i(TAG, String.format("stopConversation : %s", body));

        Runnable stopAIConversationRun = () -> {
            Request request = buildRequest(mSecretId, mSecretKey, VERSION, ACTION_STOP_AI_CONVERSATION, body,
                    mRegion, "");
            String response = "";
            try {
                ResponseBody responseBody = mClient.newCall(request).execute().body();
                response = responseBody.string();
            } catch (IOException e) {
                Logger.e(TAG, String.format("stopConversation response IOException : %s", e.getMessage()));
            } catch (NullPointerException ne) {
                Logger.e(TAG, String.format("stopConversation response NullPointerException : %s", ne.getMessage()));
            }
            Logger.i(TAG, String.format("stopConversation response : %S", response));
            mTaskId = "";
        };
        new Thread(stopAIConversationRun).start();
    }

    private String generateStartConversationBody(String roomId, AIConversationConfig config) {
        HashMap<String, Object> agentMap = new HashMap<>();
        agentMap.put("UserId", config.getAgentConfig().getAiRobotId());
        agentMap.put("UserSig", config.getAgentConfig().getAiRobotSig());
        agentMap.put("TargetUserId", TUILogin.getUserId());
        String welcomeMessage = config.getAgentConfig().getWelcomeMessage();
        if (!TextUtils.isEmpty(welcomeMessage)) {
            agentMap.put("WelcomeMessage", welcomeMessage);
        }
        agentMap.put("InterruptMode", config.getAgentConfig().getInterruptMode());
        agentMap.put("SubtitleMode", 1);

        HashMap<String, Object> sttMap = new HashMap<>();
        String asrLanguage = config.getSttConfig().getLanguage();
        if (!TextUtils.isEmpty(asrLanguage)) {
            sttMap.put("Language", asrLanguage);
        }
        sttMap.put("VadLevel", config.getSttConfig().getVadLevel());
        sttMap.put("VadSilenceTime", config.getSttConfig().getVadSilenceTime());

        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("SdkAppId", TUILogin.getSdkAppId());
        dataMap.put("RoomId", roomId);
        dataMap.put("RoomIdType", ConversationConstant.ROOM_TYPE_STRING);
        dataMap.put("AgentConfig", agentMap);
        dataMap.put("STTConfig", sttMap);
        dataMap.put("LLMConfig", config.getLlmConfig());
        dataMap.put("TTSConfig", config.getTtsConfig());
        return mGson.toJson(dataMap);
    }

    private String generateStopConversationBody() {
        HashMap<String, Object> dataMap = new HashMap<>();
        dataMap.put("TaskId", mTaskId);
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
