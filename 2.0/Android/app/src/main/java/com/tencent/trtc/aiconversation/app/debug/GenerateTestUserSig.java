// Copyright (c) 2026 Tencent. All rights reserved.
// Module:   GenerateTestUserSig
//
// Description: 在本地计算 UserSig。UserSig 是腾讯云设计的安全签名，由 SDKAppID、UserID、EXPIRETIME
// 经 HMAC-SHA256 计算得出。
//
// 注意：仅用于本地调试跑通 Demo，不要用于正式上线的应用。
//   客户端代码中的密钥很容易被反编译获取，密钥泄露后攻击者可以盗用你的云流量。
//   正式环境应把 UserSig 的计算放到业务服务端，App 需要时向服务端请求。
//   参考 https://cloud.tencent.com/document/product/647/17275
//
// 本类无状态，SDKAppID 与密钥由调用方传入；Demo 中可直接使用 AppConfig.SDK_APP_ID 与
// AppConfig.SDK_APP_KEY。
//   String sig = GenerateTestUserSig.genTestUserSig(
//           AppConfig.SDK_APP_ID, AppConfig.SDK_APP_KEY, userId);

package com.tencent.trtc.aiconversation.app.debug;

import android.util.Base64;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.zip.Deflater;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class GenerateTestUserSig {

    private static final String TAG = "GenerateTestUserSig";

    /** 签名有效期，单位秒，默认 604800（7 天），不建议设置过短。 */
    private static final int EXPIRETIME = 604800;

    private GenerateTestUserSig() {
    }

    /** SDKAppID 与密钥是否可用。 */
    public static boolean isConfigured(int sdkAppId, String secretKey) {
        return sdkAppId > 0 && secretKey != null && !secretKey.isEmpty();
    }

    /**
     * 计算 UserSig。凭证不合法时返回空字符串。
     *
     * @param sdkAppId   TRTC 应用 ID
     * @param secretKey  计算签名用的密钥
     * @param userId     用户 ID
     * @return 成功返回 UserSig，失败返回空字符串
     */
    public static String genTestUserSig(int sdkAppId, String secretKey, String userId) {
        if (!isConfigured(sdkAppId, secretKey)) {
            Log.e(TAG, "invalid sdkAppId or secretKey");
            return "";
        }
        if (userId == null || userId.isEmpty()) {
            Log.e(TAG, "userId is empty");
            return "";
        }
        return genTLSSignature(sdkAppId, userId, EXPIRETIME, null, secretKey);
    }

    /**
     * 生成 TLS 票据。
     *
     * @param sdkAppId      应用 ID
     * @param userId        用户 ID
     * @param expire        有效期，单位秒
     * @param userBuf       默认传 null
     * @param priKeyContent 计算签名用的密钥
     * @return 出错时返回空字符串，成功返回有效票据
     */
    private static String genTLSSignature(long sdkAppId, String userId, long expire, byte[] userBuf,
                                          String priKeyContent) {
        long currTime = System.currentTimeMillis() / 1000;
        JSONObject sigDoc = new JSONObject();
        try {
            sigDoc.put("TLS.ver", "2.0");
            sigDoc.put("TLS.identifier", userId);
            sigDoc.put("TLS.sdkappid", sdkAppId);
            sigDoc.put("TLS.expire", expire);
            sigDoc.put("TLS.time", currTime);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        String base64UserBuf = null;
        if (null != userBuf) {
            base64UserBuf = Base64.encodeToString(userBuf, Base64.NO_WRAP);
            try {
                sigDoc.put("TLS.userbuf", base64UserBuf);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        String sig = hmacsha256(sdkAppId, userId, currTime, expire, priKeyContent, base64UserBuf);
        if (sig.length() == 0) {
            return "";
        }
        try {
            sigDoc.put("TLS.sig", sig);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Deflater compressor = new Deflater();
        compressor.setInput(sigDoc.toString().getBytes(Charset.forName("UTF-8")));
        compressor.finish();
        byte[] compressedBytes = new byte[2048];
        int compressedBytesLength = compressor.deflate(compressedBytes);
        compressor.end();
        return new String(base64EncodeUrl(Arrays.copyOfRange(compressedBytes, 0, compressedBytesLength)));
    }

    private static String hmacsha256(long sdkAppId, String userId, long currTime, long expire,
                                     String priKeyContent, String base64UserBuf) {
        String contentToBeSigned =
                "TLS.identifier:" + userId + "\n"
                        + "TLS.sdkappid:" + sdkAppId + "\n"
                        + "TLS.time:" + currTime + "\n"
                        + "TLS.expire:" + expire + "\n";
        if (null != base64UserBuf) {
            contentToBeSigned += "TLS.userbuf:" + base64UserBuf + "\n";
        }
        try {
            byte[] byteKey = priKeyContent.getBytes("UTF-8");
            Mac hmac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(byteKey, "HmacSHA256");
            hmac.init(keySpec);
            byte[] byteSig = hmac.doFinal(contentToBeSigned.getBytes("UTF-8"));
            return Base64.encodeToString(byteSig, Base64.NO_WRAP);
        } catch (UnsupportedEncodingException | NoSuchAlgorithmException | InvalidKeyException e) {
            e.printStackTrace();
            return "";
        }
    }

    /** base64 后的 '+' '/' '=' 需替换为 '*' '-' '_'，否则无法作为 URL 参数传递。 */
    private static byte[] base64EncodeUrl(byte[] input) {
        byte[] base64 = Base64.encodeToString(input, Base64.NO_WRAP).getBytes();
        for (int i = 0; i < base64.length; ++i) {
            switch (base64[i]) {
                case '+':
                    base64[i] = '*';
                    break;
                case '/':
                    base64[i] = '-';
                    break;
                case '=':
                    base64[i] = '_';
                    break;
                default:
                    break;
            }
        }
        return base64;
    }
}
