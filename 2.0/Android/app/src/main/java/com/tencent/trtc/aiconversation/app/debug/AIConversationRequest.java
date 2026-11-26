// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation Demo - AI 对话 2.0 REST 请求（创建 / 删除语音对话任务）
//
// 安全提示：云 API 密钥放客户端仅适用于本地调试，会被打进 APK
// 且容易被反编译获取。 正式环境应由业务服务端调用本接口，App
// 只请求自己的服务端。
//
// 线程：start() / stop() 都是异步的，内部切子线程完成参数校验、签名与网络请求，
// 成功/失败统一回调到主线程；方法立即返回，可在主线程直接调用。

package com.tencent.trtc.aiconversation.app.debug;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class AIConversationRequest {

  private static final String TAG = "AIConversationRequest";

  /**
   * 调试开关：打印最终发送的请求体。请求体含 UserSig、LLM APIKey 等凭据，
   * 仅在本地排查参数时打开，正式包必须置为 false。
   */
  private static final boolean DUMP_REQUEST_PAYLOAD = false;

  // --- 腾讯云 API 3.0 公共参数 ---
  private static final String HOST = "trtc.tencentcloudapi.com";
  private static final String ENDPOINT = "https://" + HOST + "/";
  private static final String SERVICE = "trtc";
  private static final String ACTION_CREATE = "CreateVoiceConversation";
  private static final String ACTION_DELETE = "DeleteVoiceConversation";
  private static final String VERSION = "2019-07-22";
  private static final String ALGORITHM = "TC3-HMAC-SHA256";
  private static final String CONTENT_TYPE = "application/json; charset=utf-8";
  private static final String SIGNED_HEADERS = "content-type;host";

  private static final int CONNECT_TIMEOUT_MS = 10_000;
  private static final int READ_TIMEOUT_MS = 15_000;
  private static final Charset UTF8 = Charset.forName("UTF-8");
  private static final char[] HEX = "0123456789abcdef".toCharArray();

  // --- 请求体的固定配置 ---
  private static final int ROOM_ID_TYPE_STRING = 1;
  private static final String CONVERSATION_MODE_CASCADE = "Cascade";
  private static final String CONVERSATION_MODE_S2S = "S2S";
  private static final int MAX_IDLE_TIME_SECONDS = 60;
  private static final int SUBTITLE_MODE_FAST = 0;
  private static final String TURN_DETECTION_AUTO = "auto";
  private static final int INTERRUPT_MODE_AUTO = 0;
  private static final int INTERRUPT_SPEECH_DURATION_MS = 200;
  /** LLM 请求超时，2.0 单位是毫秒，范围 [1000,60000]。 */
  private static final int LLM_TIMEOUT_MS = 60_000;
  /** LLM 采样温度。 */
  private static final float LLM_TEMPERATURE = 0.7f;

  // --- STT / TTS 配置 ---
  /** ASR 服务商。 */
  public static final String STT_PROVIDER = "trtc";
  /** ASR 参数 JSON 字符串。 */
  public static final String STT_PARAMS =
      "{\"engine_model_type\":\"bigmodel\",\"language\":\"zh\","
      + "\"vad_silence_time\":\"1000\",\"max_speak_time\":"
      + "\"60000\"}";
  /** TTS 服务商。 */
  public static final String TTS_PROVIDER = "trtc";
  /** TTS 参数 JSON 字符串，默认音色；各通话页可覆盖 ttsParams 换音色。 */
  public static final String TTS_PARAMS =
      "{\"model\":\"flow_02_turbo\",\"voice_id\":\"v-female-R2s4N9qJ\","
      + "\"speed\":1.0,\"volume\":1.0,\"pitch\":0.0}";
  /** TTS 推流机器人 UserId，作为 AgentConfig.TTSPushRoomParams.UserId。 */
  private static final String TTS_BOT_USER_ID = "tts-bot-001";

  // --- S2S（端到端）配置 ---
  // trtc-hy（TRTC 自研 S2S / 混元）的 Params 合法字段由云 API 校验：
  //   voice / instructions / vad_type / asr / hosted_search
  // 注意：文档 140385 的 Model / APIKey / SystemPrompt / VoiceId 是 doubao 的字段，不适用于 trtc-hy。
  /** S2S 服务商。 */
  public static final String S2S_PROVIDER = "trtc-hy";
  /** S2S 音色（Params.voice）。 */
  private static final String S2S_VOICE = "f1";
  /** S2S 托管联网搜索（Params.hosted_search）。 */
  private static final boolean S2S_HOSTED_SEARCH = true;
  /** S2S 默认欢迎语（S2SConfig.WelcomeMessage），场景未指定欢迎语时使用。 */
  private static final String S2S_DEFAULT_WELCOME = "你好呀";

  // --- 回调错误码 ---
  /** 云 API 凭据（SecretId / SecretKey / Region）未传或为空。 */
  public static final int ERR_NOT_CONFIGURED = -1;
  /** 请求参数不满足协议必填项 / 取值非法。 */
  public static final int ERR_INVALID_PARAM = -2;
  /** HTTP 非 2xx。 */
  public static final int ERR_HTTP = -3;
  /** HTTP 200，但云 API 返回了 Error，或响应格式异常。 */
  public static final int ERR_API = -4;
  /** 网络 IO / 签名计算异常。 */
  public static final int ERR_NETWORK = -5;

  private static final ExecutorService EXECUTOR =
      Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "aic-cloud-api");
        t.setDaemon(true);
        return t;
      });

  private AIConversationRequest() {}

  // ==================================================================
  // 请求参数（按 140624 / 140623 输入参数 + 44055 数据结构）
  // ==================================================================

  /** 云 API 公共参数：TC3 签名所需的密钥与地域。 */
  public static final class Credentials {
    public final String secretId;
    public final String secretKey;
    public final String region;

    public Credentials(String secretId, String secretKey, String region) {
      this.secretId = secretId;
      this.secretKey = secretKey;
      this.region = region;
    }
  }

  /**
   * 创建 AI 对话任务的入参：由调用方一次性传入全部配置值（SDKAppId、LLM
   * 配置）和本次会话的动态值（房间号、机器人身份、场景提示词 / 音色）。
   * {@link #start(Credentials, RequestParams, Callback)} 只用这些值组装请求体，
   */
  public static final class RequestParams {
    /** 必选：TRTC 应用 ID。 */
    public int sdkAppId;
    /** 必选：大模型类型，如 "volcengine"。 */
    public String llmType;
    /** 必选：模型名称。 */
    public String llmModel;
    /** 模型 API 地址。 */
    public String llmApiUrl;
    /** 模型 API 密钥。 */
    public String llmApiKey;

    /** 必选：TRTC 房间号。 */
    public String roomId;
    /** 必选：AI 机器人 UserId。 */
    public String agentUserId;
    /** 必选：AI 机器人 UserId 对应的 UserSig。 */
    public String agentUserSig;
    /** 必选：机器人拉流目标，即本地用户 UserId。 */
    public String targetUserId;
    /** 场景系统提示词，由各通话页覆盖。 */
    public String llmSystemPrompt;
    /** 场景 TTS 参数 JSON，由各通话页覆盖。 */
    public String ttsParams;
    /**
     * 场景欢迎语。级联模式下发到 DialogConfig.WelcomeMessage，为空则不下发；
     * S2S 模式下发到 S2SConfig.WelcomeMessage，为空时使用默认欢迎语。
     */
    public String welcomeMessage;
    /**
     * 是否使用端到端（S2S）模式。true 时只下发 S2SConfig（llmSystemPrompt 作为
     * instructions），不下发 STT / LLM / TTS / Dialog / Vision 配置，LLM 相关字段可不填。
     */
    public boolean s2s;
  }

  /**
   * 140624 CreateVoiceConversation 输入参数。
   * 必填：sdkAppId、roomId、roomIdType、agentConfig。
   */
  public static final class Params {
    /** 必选 Integer：TRTC 应用 ID。 */
    public int sdkAppId;
    /** 必选 String：TRTC 房间号。 */
    public String roomId;
    /** 必选 Integer：0 数字房间号，1 字符串房间号。 */
    public Integer roomIdType;
    /** 必选 [AgentConfig]：AI 机器人配置。 */
    public AgentConfig agentConfig;
    /** 可选 String："Cascade"（级联，默认）或 "S2S"（端到端）。 */
    public String conversationMode;
    /** S2S 模式下必选：端到端模型配置。 */
    public S2SConfig s2sConfig;
    /** 可选：语音识别配置，Cascade 模式生效。 */
    public STTConfig sttConfig;
    /** 可选：大语言模型配置，Cascade 模式生效。 */
    public LLMConfig llmConfig;
    /** 可选：文本转语音配置，Cascade 模式生效。 */
    public TTSConfig ttsConfig;
    /** 可选：对话管理配置。 */
    public DialogConfig dialogConfig;
    /** 可选：视觉理解配置。 */
    public VisionConfig visionConfig;
  }

  /** 140623 DeleteVoiceConversation 输入参数：SdkAppId + TaskId 都必填。 */
  public static final class StopParams {
    /** 必选 Integer：与创建任务时相同的 TRTC 应用 ID。 */
    public int sdkAppId;
    /** 必选 String：CreateVoiceConversation 返回的 TaskId。 */
    public String taskId;

    public StopParams() {}

    public StopParams(int sdkAppId, String taskId) {
      this.sdkAppId = sdkAppId;
      this.taskId = taskId;
    }
  }

  /** 44055 VoiceConversationConfig：AI 机器人配置。 */
  public static final class AgentConfig {
    /** 必选：机器人 UserId，房间内必须唯一。 */
    public String userId;
    /** 必选：机器人 UserId 对应的 UserSig。 */
    public String userSig;
    /** 必选 Array of String：机器人拉流目标，当前仅支持一个。 */
    public List<String> targetUserId;
    /** 必选 [TranscriptionUserInfoParams]：TTS 推流机器人信息。 */
    public TranscriptionUserInfoParams ttsPushRoomParams;
    /** 可选：无推流超过该秒数自动关闭任务，范围 [5,86400]，默认 30。 */
    public Integer maxIdleTime;
    /** 可选 Array of Integer：过滤 LLM 输出中的括号内容，1~5。 */
    public List<Integer> filterBracketsContent;
    /** 可选：字幕模式，0 尽快显示，1 句子级实时显示。 */
    public Integer subtitleMode;
    /** 可选：断句模式，"auto" / "manual"；S2S 模式下云 API 不允许下发。 */
    public String turnDetectionMode;
  }

  /** 44055 TranscriptionUserInfoParams：TTS 推流机器人信息。 */
  public static final class TranscriptionUserInfoParams {
    /** 必选：TTS 推流机器人 UserId。 */
    public String userId;

    public TranscriptionUserInfoParams() {}

    public TranscriptionUserInfoParams(String userId) { this.userId = userId; }
  }

  /** 44055 VoiceConversationS2SConfig：端到端模型配置。 */
  public static final class S2SConfig {
    /** 必选：端到端模型服务商。 */
    public String provider;
    /** 必选：模型参数，JSON 字符串。 */
    public String params;
    /** 可选：欢迎语。 */
    public String welcomeMessage;
  }

  /** 44055 VoiceConversationSTTConfig：语音转文字配置。 */
  public static final class STTConfig {
    /** 必选：ASR 服务商，如 "trtc"。 */
    public String provider;
    /** 必选：ASR 参数，JSON 字符串。 */
    public String params;
  }

  /** 44055 VoiceConversationTTSConfig：文本转语音配置。 */
  public static final class TTSConfig {
    /** 必选：TTS 服务商，如 "trtc"。 */
    public String provider;
    /** 必选：TTS 参数，JSON 字符串。 */
    public String params;
  }

  /**
   * 44055 VoiceConversationLLMConfig：大模型配置（2.0 里是对象，不是字符串）。
   */
  public static final class LLMConfig {
    /** 必选：大模型类型，如 "openai"。 */
    public String llmType;
    /** 必选：模型名称。 */
    public String model;
    /** 模型 API 地址。 */
    public String apiUrl;
    /** 模型 API 密钥。 */
    public String apiKey;
    /** 系统提示词。 */
    public String systemPrompt;
    /** 请求失败时的兜底回复。 */
    public String failureMessage;
    /** 单次回答最大 Token 数，0=不限制，默认 0。 */
    public Integer maxTokens;
    /** 是否开启深度思考。 */
    public Boolean thinkingMode;
    /** LLM 缓存的对话轮次，范围 0~50。 */
    public Integer maxHistory;
    /** MCP 工具调用最大轮次，范围 0~50，默认 10。 */
    public Integer maxToolRound;
    /** 请求超时，单位毫秒，范围 1000~60000，默认 10000。 */
    public Integer timeout;
    /** 采样温度，范围 0.0~2.0，默认 1.0。 */
    public Float temperature;
    /** 采样概率，范围 0.0~1.0，默认 1.0。 */
    public Float topP;
    /** MCP 服务配置。 */
    public McpServer mcpServer;
    /** 额外 HTTP 头。 */
    public List<KeyValueItem> customHeaders;
    /** 额外请求字段。 */
    public List<KeyValueItem> customFields;
    /** 预置历史消息。 */
    public List<HistoryMessage> historyMessages;
  }

  /** 44055 VoiceConversationMcpServer：MCP 工具服务。 */
  public static final class McpServer {
    public String name;
    public String url;
    public Integer timeout;
    public List<KeyValueItem> headers;
    public List<String> toolList;
  }

  /** 44055 KeyValueItem：键值对。 */
  public static final class KeyValueItem {
    public String key;
    public String value;

    public KeyValueItem() {}

    public KeyValueItem(String key, String value) {
      this.key = key;
      this.value = value;
    }
  }

  /** 44055 VoiceConversationHistoryMessage：预置历史消息。 */
  public static final class HistoryMessage {
    public String role;
    public String content;

    public HistoryMessage() {}

    public HistoryMessage(String role, String content) {
      this.role = role;
      this.content = content;
    }
  }

  /** 44055 VoiceConversationDialogConfig：对话管理配置。 */
  public static final class DialogConfig {
    /** 0 服务端自动打断（默认），1 端上发打断信令。 */
    public Integer interruptMode;
    /** 打断判定语音时长，单位 ms，默认 200。 */
    public Integer interruptSpeechDuration;
    /** 打断关键词列表。 */
    public List<String> interruptKeywords;
    /** 静音多少秒后 Bot 主动追问，范围 0~300，0 表示关闭（默认）。 */
    public Integer silenceFollowUp;
    /** 静音追问文案。 */
    public String silenceFollowUpText;
    /** 欢迎语。 */
    public String welcomeMessage;
  }

  /** 44055 VoiceConversationVisionConfig：视觉理解配置。 */
  public static final class VisionConfig {
    /** 必选：是否启用视觉理解。 */
    public Boolean enable;
    /** Enable=true 时必选：存储类型，0 不落存储，1 落云存储。 */
    public Integer storageType;
    /** StorageType=1 时必选：云存储配置。 */
    public CloudStorage cloudStorage;
  }

  /** 44055 CloudStorage：视觉输入落存储配置。 */
  public static final class CloudStorage {
    public Integer vendor;
    public String region;
    public String bucket;
    public String accessKey;
    public String secretKey;
    /** Array of String：存储路径前缀。 */
    public List<String> fileNamePrefix;
  }

  public interface Callback {
    /** 请求成功，在主线程回调，返回 TaskId（stop 时回显入参）。 */
    void onSuccess(String taskId);

    /** 失败，errorCode 见 ERR_* 常量；在主线程回调。 */
    void onFailure(int errorCode, String message);
  }

  @FunctionalInterface
  private interface RequestAction {
    String run() throws StartException;
  }

  // ==================================================================
  // 对外入口（均为异步）
  // ==================================================================

  /**
   * 异步创建 AI 对话任务（CreateVoiceConversation，140624）。
   * 请求体在本方法内部组装：全部配置与会话动态值都取自 requestParams；
   * 随后完成参数校验、TC3 签名与 HTTPS 请求，结果回调到主线程。
   *
   * @param credentials 云 API 签名凭据与地域
   * @param context     本次会话的房间号、机器人身份与场景配置
   * @param callback    成功时返回 TaskId；可为 null
   */
  public static void start(Credentials credentials, RequestParams context,
                           Callback callback) {
    runAsync(callback, () -> {
      validateContext(context);
      Params params = buildParams(context);
      validateStart(credentials, params);
      JSONObject response =
          execute(credentials, ACTION_CREATE, buildPayload(params));
      return parseTaskId(response, ACTION_CREATE);
    });
  }

  /**
   * 异步删除 AI 对话任务（DeleteVoiceConversation，140623）。
   * 请求体为 {@code {"SdkAppId":..., "TaskId":"..."}}，响应含 TaskId 与
   * RequestId。
   * 建议在用户结束对话、离开通话页时调用，避免任务继续占用计费资源。
   */
  public static void stop(Credentials credentials, StopParams params,
                          Callback callback) {
    runAsync(callback, () -> {
      validateStop(credentials, params);
      JSONObject response =
          execute(credentials, ACTION_DELETE, buildStopPayload(params));
      Log.i(TAG, ACTION_DELETE + " 成功，TaskId=" + params.taskId +
                     "，RequestId=" + response.optString("RequestId", ""));
      return params.taskId;
    });
  }

  /** start() / stop() 共用的异步执行骨架：子线程执行，主线程回调。 */
  private static void runAsync(Callback callback, RequestAction action) {
    Handler mainHandler = new Handler(Looper.getMainLooper());
    EXECUTOR.execute(() -> {
      try {
        String taskId = action.run();
        mainHandler.post(() -> {
          if (callback != null)
            callback.onSuccess(taskId);
        });
      } catch (StartException e) {
        postFailure(mainHandler, callback, e.errorCode, e.getMessage());
      } catch (RuntimeException e) {
        // 兜底：避免工作线程因未预期异常静默退出、导致调用方永远收不到回调
        Log.e(TAG, "unexpected error: " + e.getMessage());
        postFailure(mainHandler, callback, ERR_NETWORK,
                    "请求异常：" + e.getMessage());
      }
    });
  }

  private static void postFailure(Handler mainHandler, Callback callback,
                                  int errorCode, String message) {
    mainHandler.post(() -> {
      if (callback != null)
        callback.onFailure(errorCode, message);
    });
  }

  /**
   * 在工作线程执行签名 + POST + 公共响应校验；返回 Response
   * 对象供各接口提取字段。
   */
  private static JSONObject execute(Credentials credentials, String action,
                                    String payload) throws StartException {
    final long timestamp = System.currentTimeMillis() / 1000L;
    try {
      String authorization =
          buildAuthorization(payload, timestamp, credentials);
      String body =
          post(payload, authorization, timestamp, credentials.region, action);
      return parseResponse(body);
    } catch (GeneralSecurityException e) {
      Log.e(TAG, "sign failed: " + e.getMessage());
      throw new StartException(ERR_NETWORK, "签名计算失败：" + e.getMessage());
    } catch (IOException e) {
      Log.e(TAG, "request failed: " + e.getMessage());
      throw new StartException(ERR_NETWORK, "网络请求失败：" + e.getMessage());
    }
  }

  private static void validateCredentials(Credentials credentials)
      throws StartException {
    if (credentials == null || isBlank(credentials.secretId) ||
        isBlank(credentials.secretKey) || isBlank(credentials.region)) {
      throw new StartException(
          ERR_NOT_CONFIGURED, "Credentials 需要 SecretId / SecretKey / Region");
    }
  }

  /** 按 140624 必填项校验。 */
  private static void validateStart(Credentials credentials, Params params)
      throws StartException {
    validateCredentials(credentials);
    if (params == null) {
      throw new StartException(ERR_INVALID_PARAM, "Params 不能为空");
    }
    if (params.sdkAppId <= 0) {
      throw new StartException(ERR_INVALID_PARAM, "SdkAppId 必须为正数");
    }
    if (isBlank(params.roomId)) {
      throw new StartException(ERR_INVALID_PARAM, "RoomId 不能为空");
    }
    if (params.roomIdType == null ||
        (params.roomIdType != 0 && params.roomIdType != 1)) {
      throw new StartException(ERR_INVALID_PARAM,
                               "RoomIdType 必填，且只能为 0 或 1");
    }
    AgentConfig agent = params.agentConfig;
    if (agent == null || isBlank(agent.userId) || isBlank(agent.userSig)) {
      throw new StartException(ERR_INVALID_PARAM,
                               "AgentConfig 需要 UserId / UserSig");
    }
    if (agent.targetUserId == null || agent.targetUserId.isEmpty()) {
      throw new StartException(
          ERR_INVALID_PARAM, "AgentConfig.TargetUserId 必填（当前仅支持一个）");
    }
    for (String target : agent.targetUserId) {
      if (!isBlank(target) && target.trim().equals(agent.userId.trim())) {
        throw new StartException(
            ERR_INVALID_PARAM,
            "机器人 UserId 不能与 TargetUserId 相同（房间内必须唯一）");
      }
    }
    if (agent.ttsPushRoomParams == null ||
        isBlank(agent.ttsPushRoomParams.userId)) {
      throw new StartException(ERR_INVALID_PARAM,
                               "AgentConfig.TTSPushRoomParams.UserId 必填");
    }
    boolean s2s = "S2S".equalsIgnoreCase(params.conversationMode);
    if (s2s) {
      if (params.s2sConfig == null) {
        throw new StartException(ERR_INVALID_PARAM,
                                 "ConversationMode=S2S 时 S2SConfig 必填");
      }
    } else if (params.sttConfig == null || params.llmConfig == null ||
               params.ttsConfig == null) {
      // 140624：STTConfig / LLMConfig / TTSConfig 仅 Cascade
      // 模式必填（默认模式）
      throw new StartException(
          ERR_INVALID_PARAM,
          "ConversationMode=Cascade 时 STTConfig / LLMConfig / TTSConfig 必填");
    }
    if (params.visionConfig != null && params.visionConfig.enable == null) {
      throw new StartException(ERR_INVALID_PARAM, "VisionConfig.Enable 必填");
    }
    validateProvider("STTConfig",
                     params.sttConfig == null ? null
                                              : params.sttConfig.provider,
                     params.sttConfig == null ? null : params.sttConfig.params);
    validateProvider("TTSConfig",
                     params.ttsConfig == null ? null
                                              : params.ttsConfig.provider,
                     params.ttsConfig == null ? null : params.ttsConfig.params);
    if (params.s2sConfig != null) {
      validateProvider("S2SConfig", params.s2sConfig.provider,
                       params.s2sConfig.params);
    }
  }

  private static void validateProvider(String name, String provider,
                                       String params) throws StartException {
    if (isBlank(provider) && isBlank(params)) {
      return; // 整个配置未下发
    }
    if (isBlank(provider) || isBlank(params)) {
      throw new StartException(ERR_INVALID_PARAM,
                               name + " 的 Provider / Params 必须同时填写");
    }
  }

  /** 按 140623 必填项校验：SdkAppId + TaskId。 */
  private static void validateStop(Credentials credentials, StopParams params)
      throws StartException {
    validateCredentials(credentials);
    if (params == null || isBlank(params.taskId)) {
      throw new StartException(ERR_INVALID_PARAM, "TaskId 不能为空");
    }
    if (params.sdkAppId <= 0) {
      throw new StartException(ERR_INVALID_PARAM, "SdkAppId 必须为正数");
    }
  }

  // ------------------------------------------------------------------
  // 请求体组装
  // ------------------------------------------------------------------

  /**
   * 按 140624 组装完整 Params：全部取值都来自 RequestParams 与类内常量。
   * 按 {@link RequestParams#s2s} 选择级联或端到端。
   */
  private static Params buildParams(RequestParams c) throws StartException {
    Params p = new Params();
    p.sdkAppId = c.sdkAppId;
    p.roomId = c.roomId;
    p.roomIdType = ROOM_ID_TYPE_STRING;

    AgentConfig agent = new AgentConfig();
    agent.userId = c.agentUserId;
    agent.userSig = c.agentUserSig;
    agent.targetUserId = Collections.singletonList(c.targetUserId);
    agent.ttsPushRoomParams = new TranscriptionUserInfoParams(TTS_BOT_USER_ID);
    agent.maxIdleTime = MAX_IDLE_TIME_SECONDS;
    p.agentConfig = agent;

    if (c.s2s) {
      // S2S：只带 S2SConfig，不带 STT / LLM / TTS / Dialog / Vision（见 140332 / 140385）。
      // AgentConfig.SubtitleMode / TurnDetectionMode 在 S2S 下会被云 API 拒绝
      // （InvalidParameter），不能下发；AgentConfig 只保留与 index.py 一致的字段。
      p.conversationMode = CONVERSATION_MODE_S2S;
      S2SConfig s2s = new S2SConfig();
      s2s.provider = S2S_PROVIDER;
      s2s.params = buildS2SParams(c.llmSystemPrompt);
      s2s.welcomeMessage =
          isBlank(c.welcomeMessage) ? S2S_DEFAULT_WELCOME : c.welcomeMessage;
      p.s2sConfig = s2s;
      return p;
    }

    p.conversationMode = CONVERSATION_MODE_CASCADE;
    // 字幕模式、断句模式与括号过滤都只对级联下发
    agent.subtitleMode = SUBTITLE_MODE_FAST;
    agent.turnDetectionMode = TURN_DETECTION_AUTO;
    agent.filterBracketsContent = Arrays.asList(1, 2);

    STTConfig stt = new STTConfig();
    stt.provider = STT_PROVIDER;
    stt.params = STT_PARAMS;
    p.sttConfig = stt;

    LLMConfig llm = new LLMConfig();
    llm.llmType = c.llmType;
    llm.model = c.llmModel;
    llm.apiUrl = c.llmApiUrl;
    llm.apiKey = c.llmApiKey;
    llm.timeout = LLM_TIMEOUT_MS;
    llm.temperature = LLM_TEMPERATURE;
    llm.systemPrompt = c.llmSystemPrompt;
    p.llmConfig = llm;

    TTSConfig tts = new TTSConfig();
    tts.provider = TTS_PROVIDER;
    tts.params = c.ttsParams;
    p.ttsConfig = tts;

    DialogConfig dialog = new DialogConfig();
    dialog.interruptMode = INTERRUPT_MODE_AUTO;
    dialog.interruptSpeechDuration = INTERRUPT_SPEECH_DURATION_MS;
    dialog.welcomeMessage = c.welcomeMessage;
    p.dialogConfig = dialog;

    VisionConfig vision = new VisionConfig();
    vision.enable = true;
    p.visionConfig = vision;
    return p;
  }

  /**
   * 组装 trtc-hy 的 S2SConfig.Params（JSON 字符串）：voice + hosted_search 固定下发，
   * instructions（系统提示词）仅在非空时下发。
   */
  private static String buildS2SParams(String instructions)
      throws StartException {
    try {
      JSONObject o = new JSONObject();
      o.put("voice", S2S_VOICE);
      o.put("hosted_search", S2S_HOSTED_SEARCH);
      putIfNotBlank(o, "instructions", instructions);
      return o.toString();
    } catch (JSONException e) {
      throw new StartException(ERR_API, "组装 S2S 参数失败：" + e.getMessage());
    }
  }

  /** 入参必填项校验；协议层必填由 validateStart(Params) 统一覆盖。 */
  private static void validateContext(RequestParams c) throws StartException {
    if (c == null || isBlank(c.roomId) || isBlank(c.agentUserId) ||
        isBlank(c.agentUserSig) || isBlank(c.targetUserId)) {
      throw new StartException(
          ERR_INVALID_PARAM,
          "RequestParams 需要 RoomId / 机器人身份 / 目标用户");
    }
    if (c.sdkAppId <= 0) {
      throw new StartException(ERR_INVALID_PARAM, "RequestParams 需要 SdkAppId");
    }
    // LLM 配置只有级联模式需要
    if (!c.s2s && (isBlank(c.llmType) || isBlank(c.llmModel))) {
      throw new StartException(ERR_INVALID_PARAM,
                               "级联模式需要 LLMType / Model");
    }
  }

  /** 组装 DeleteVoiceConversation 请求体：{"SdkAppId":..., "TaskId":"..."}。 */
  private static String buildStopPayload(StopParams p) throws StartException {
    try {
      JSONObject root = new JSONObject();
      root.put("SdkAppId", p.sdkAppId);
      root.put("TaskId", p.taskId);
      return root.toString();
    } catch (JSONException e) {
      throw new StartException(ERR_API, "组装请求体失败：" + e.getMessage());
    }
  }

  /**
   * 组装 CreateVoiceConversation 的 JSON 请求体，并按
   * {@link #DUMP_REQUEST_PAYLOAD} 打印最终发送的数据。
   *
   * <p>注意：payload 内含 LLM APIKey、UserSig
   * 等敏感信息，打印仅用于本地排查参数。 正式包必须把 DUMP_REQUEST_PAYLOAD 置为
   * false。
   */
  private static String buildPayload(Params p) throws StartException {
    try {
      JSONObject root = new JSONObject();
      root.put("SdkAppId", p.sdkAppId);
      root.put("RoomId", p.roomId);
      root.put("RoomIdType", p.roomIdType);
      root.put("AgentConfig", buildAgentConfig(p.agentConfig));
      putIfNotBlank(root, "ConversationMode", p.conversationMode);
      if (p.s2sConfig != null) {
        root.put("S2SConfig", buildS2SConfig(p.s2sConfig));
      }
      if (p.sttConfig != null) {
        JSONObject o = new JSONObject();
        o.put("Provider", p.sttConfig.provider);
        o.put("Params", p.sttConfig.params);
        root.put("STTConfig", o);
      }
      if (p.llmConfig != null) {
        root.put("LLMConfig", buildLlmConfig(p.llmConfig));
      }
      if (p.ttsConfig != null) {
        JSONObject o = new JSONObject();
        o.put("Provider", p.ttsConfig.provider);
        o.put("Params", p.ttsConfig.params);
        root.put("TTSConfig", o);
      }
      if (p.dialogConfig != null) {
        root.put("DialogConfig", buildDialogConfig(p.dialogConfig));
      }
      if (p.visionConfig != null) {
        root.put("VisionConfig", buildVisionConfig(p.visionConfig));
      }
      String payload = root.toString();
      if (DUMP_REQUEST_PAYLOAD) {
        logPayload(ACTION_CREATE, payload);
      }
      return payload;
    } catch (JSONException e) {
      throw new StartException(ERR_API, "组装请求体失败：" + e.getMessage());
    }
  }

  /**
   * 打印最终发送的 JSON。logcat 单条有长度上限（约 4KB），SystemPrompt 较长时
   * 整条会被截断、看不到尾部字段，因此按固定长度分片输出，每片带序号。
   */
  private static void logPayload(String action, String payload) {
    Log.i(TAG, "PAYLOAD " + action + ", len=" + payload.length());
    final int chunkSize = 1000;
    int total = (payload.length() + chunkSize - 1) / chunkSize;
    for (int i = 0; i < total; i++) {
      int start = i * chunkSize;
      int end = Math.min(start + chunkSize, payload.length());
      Log.i(TAG, "PAYLOAD[" + (i + 1) + "/" + total +
                     "]: " + payload.substring(start, end));
    }
  }

  private static JSONObject buildAgentConfig(AgentConfig a)
      throws JSONException {
    JSONObject o = new JSONObject();
    o.put("UserId", a.userId);
    o.put("UserSig", a.userSig);
    putStringList(o, "TargetUserId", a.targetUserId);

    JSONObject push = new JSONObject();
    push.put("UserId", a.ttsPushRoomParams.userId);
    o.put("TTSPushRoomParams", push);

    putIfNotNull(o, "MaxIdleTime", a.maxIdleTime);
    putIntegerList(o, "FilterBracketsContent", a.filterBracketsContent);
    putIfNotNull(o, "SubtitleMode", a.subtitleMode);
    putIfNotBlank(o, "TurnDetectionMode", a.turnDetectionMode);
    return o;
  }

  private static JSONObject buildS2SConfig(S2SConfig s) throws JSONException {
    JSONObject o = new JSONObject();
    o.put("Provider", s.provider);
    o.put("Params", s.params);
    putIfNotBlank(o, "WelcomeMessage", s.welcomeMessage);
    return o;
  }

  private static JSONObject buildLlmConfig(LLMConfig l) throws JSONException {
    JSONObject o = new JSONObject();
    o.put("LLMType", l.llmType);
    o.put("Model", l.model);
    putIfNotBlank(o, "APIUrl", l.apiUrl);
    putIfNotBlank(o, "APIKey", l.apiKey);
    putIfNotBlank(o, "SystemPrompt", l.systemPrompt);
    putIfNotBlank(o, "FailureMessage", l.failureMessage);
    putIfNotNull(o, "MaxTokens", l.maxTokens);
    putIfNotNull(o, "ThinkingMode", l.thinkingMode);
    putIfNotNull(o, "MaxHistory", l.maxHistory);
    putIfNotNull(o, "MaxToolRound", l.maxToolRound);
    putIfNotNull(o, "Timeout", l.timeout);
    putIfNotNull(o, "Temperature", l.temperature);
    putIfNotNull(o, "TopP", l.topP);

    if (l.mcpServer != null) {
      JSONObject m = new JSONObject();
      putIfNotBlank(m, "Name", l.mcpServer.name);
      putIfNotBlank(m, "Url", l.mcpServer.url);
      putIfNotNull(m, "Timeout", l.mcpServer.timeout);
      putKeyValueList(m, "Headers", l.mcpServer.headers);
      putStringList(m, "ToolList", l.mcpServer.toolList);
      o.put("McpServer", m);
    }
    putKeyValueList(o, "CustomHeaders", l.customHeaders);
    putKeyValueList(o, "CustomFields", l.customFields);
    if (l.historyMessages != null && !l.historyMessages.isEmpty()) {
      JSONArray arr = new JSONArray();
      for (HistoryMessage h : l.historyMessages) {
        if (h == null)
          continue;
        JSONObject item = new JSONObject();
        putIfNotBlank(item, "Role", h.role);
        putIfNotBlank(item, "Content", h.content);
        arr.put(item);
      }
      o.put("HistoryMessages", arr);
    }
    return o;
  }

  private static JSONObject buildDialogConfig(DialogConfig d)
      throws JSONException {
    JSONObject o = new JSONObject();
    putIfNotNull(o, "InterruptMode", d.interruptMode);
    putIfNotNull(o, "InterruptSpeechDuration", d.interruptSpeechDuration);
    putStringList(o, "InterruptKeywords", d.interruptKeywords);
    putIfNotNull(o, "SilenceFollowUp", d.silenceFollowUp);
    putIfNotBlank(o, "SilenceFollowUpText", d.silenceFollowUpText);
    putIfNotBlank(o, "WelcomeMessage", d.welcomeMessage);
    return o;
  }

  private static JSONObject buildVisionConfig(VisionConfig v)
      throws JSONException {
    JSONObject o = new JSONObject();
    o.put("Enable", v.enable);
    putIfNotNull(o, "StorageType", v.storageType);
    if (v.cloudStorage != null) {
      JSONObject c = new JSONObject();
      putIfNotNull(c, "Vendor", v.cloudStorage.vendor);
      putIfNotBlank(c, "Region", v.cloudStorage.region);
      putIfNotBlank(c, "Bucket", v.cloudStorage.bucket);
      putIfNotBlank(c, "AccessKey", v.cloudStorage.accessKey);
      putIfNotBlank(c, "SecretKey", v.cloudStorage.secretKey);
      putStringList(c, "FileNamePrefix", v.cloudStorage.fileNamePrefix);
      o.put("CloudStorage", c);
    }
    return o;
  }

  private static void putIfNotNull(JSONObject o, String key, Object value)
      throws JSONException {
    if (value != null)
      o.put(key, value);
  }

  private static void putIfNotBlank(JSONObject o, String key, String value)
      throws JSONException {
    if (!isBlank(value))
      o.put(key, value);
  }

  private static void putStringList(JSONObject o, String key,
                                    List<String> values) throws JSONException {
    if (values == null || values.isEmpty())
      return;
    JSONArray array = new JSONArray();
    for (String value : values) {
      if (value != null)
        array.put(value);
    }
    o.put(key, array);
  }

  private static void putIntegerList(JSONObject o, String key,
                                     List<Integer> values)
      throws JSONException {
    if (values == null || values.isEmpty())
      return;
    JSONArray array = new JSONArray();
    for (Integer value : values) {
      if (value != null)
        array.put(value);
    }
    o.put(key, array);
  }

  private static void putKeyValueList(JSONObject o, String key,
                                      List<KeyValueItem> values)
      throws JSONException {
    if (values == null || values.isEmpty())
      return;
    JSONArray array = new JSONArray();
    for (KeyValueItem item : values) {
      if (item == null)
        continue;
      JSONObject kv = new JSONObject();
      putIfNotBlank(kv, "Key", item.key);
      putIfNotBlank(kv, "Value", item.value);
      array.put(kv);
    }
    o.put(key, array);
  }

  // ------------------------------------------------------------------
  // TC3-HMAC-SHA256 签名（腾讯云 API 3.0 签名方法 v3）
  // ------------------------------------------------------------------

  private static String buildAuthorization(String payload, long timestamp,
                                           Credentials credentials)
      throws GeneralSecurityException {
    String date = utcDate(timestamp);
    String credentialScope = date + "/" + SERVICE + "/tc3_request";

    // CanonicalHeaders 中的 header
    // 名必须小写、值去首尾空格、按字典序排列、每行以 \n 结尾
    String canonicalHeaders = "content-type:" + CONTENT_TYPE + "\n"
                              + "host:" + HOST + "\n";
    String canonicalRequest = "POST\n/\n\n" + canonicalHeaders + "\n" +
                              SIGNED_HEADERS + "\n" + sha256Hex(payload);

    String stringToSign = ALGORITHM + "\n" + timestamp + "\n" +
                          credentialScope + "\n" + sha256Hex(canonicalRequest);

    byte[] secretDate =
        hmacSha256(("TC3" + credentials.secretKey).getBytes(UTF8), date);
    byte[] secretService = hmacSha256(secretDate, SERVICE);
    byte[] secretSigning = hmacSha256(secretService, "tc3_request");
    String signature = toLowerHex(hmacSha256(secretSigning, stringToSign));

    return ALGORITHM + " Credential=" + credentials.secretId + "/" +
        credentialScope + ", SignedHeaders=" + SIGNED_HEADERS +
        ", Signature=" + signature;
  }

  private static String utcDate(long timestampSeconds) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
    sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
    return sdf.format(new Date(timestampSeconds * 1000L));
  }

  private static byte[] hmacSha256(byte[] key, String message)
      throws GeneralSecurityException {
    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(key, "HmacSHA256"));
    return mac.doFinal(message.getBytes(UTF8));
  }

  private static String sha256Hex(String text) throws GeneralSecurityException {
    return toLowerHex(
        MessageDigest.getInstance("SHA-256").digest(text.getBytes(UTF8)));
  }

  private static String toLowerHex(byte[] bytes) {
    char[] out = new char[bytes.length * 2];
    for (int i = 0; i < bytes.length; i++) {
      int v = bytes[i] & 0xFF;
      out[i * 2] = HEX[v >>> 4];
      out[i * 2 + 1] = HEX[v & 0x0F];
    }
    return new String(out);
  }

  // ------------------------------------------------------------------
  // HTTPS POST
  // ------------------------------------------------------------------

  private static String post(String payload, String authorization,
                             long timestamp, String region, String action)
      throws IOException, StartException {
    HttpURLConnection conn = null;
    try {
      conn = (HttpURLConnection) new URL(ENDPOINT).openConnection();
      conn.setRequestMethod("POST");
      conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
      conn.setReadTimeout(READ_TIMEOUT_MS);
      conn.setDoOutput(true);
      conn.setUseCaches(false);
      // Content-Type 必须与参与签名的 canonicalHeaders 完全一致
      conn.setRequestProperty("Content-Type", CONTENT_TYPE);
      conn.setRequestProperty("Authorization", authorization);
      conn.setRequestProperty("X-TC-Action", action);
      conn.setRequestProperty("X-TC-Timestamp", String.valueOf(timestamp));
      conn.setRequestProperty("X-TC-Version", VERSION);
      conn.setRequestProperty("X-TC-Region", region);

      try (OutputStream os = conn.getOutputStream()) {
        os.write(payload.getBytes(UTF8));
      }

      int httpCode = conn.getResponseCode();
      InputStream is = (httpCode >= 200 && httpCode < 300)
                           ? conn.getInputStream()
                           : conn.getErrorStream();
      String body = readAll(is);
      if (httpCode < 200 || httpCode >= 300) {
        throw new StartException(ERR_HTTP,
                                 "HTTP " + httpCode + "：" + truncate(body));
      }
      return body;
    } catch (StartException e) {
      throw e;
    } finally {
      if (conn != null)
        conn.disconnect();
    }
  }

  private static String readAll(InputStream is) throws IOException {
    if (is == null)
      return "";
    try (InputStream input = is;
         ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
      byte[] buf = new byte[4096];
      int n;
      while ((n = input.read(buf)) != -1) {
        bos.write(buf, 0, n);
      }
      return new String(bos.toByteArray(), UTF8);
    }
  }

  // ------------------------------------------------------------------
  // 响应解析
  // ------------------------------------------------------------------

  /**
   * 校验所有云 API 共用的响应外壳。
   * 正常：{"Response":{...}}；业务失败：{"Response":{"Error":{"Code":..,"Message":..}}}。
   */
  private static JSONObject parseResponse(String body) throws StartException {
    try {
      JSONObject response = new JSONObject(body).optJSONObject("Response");
      if (response == null) {
        throw new StartException(ERR_API,
                                 "响应缺少 Response 字段：" + truncate(body));
      }
      JSONObject error = response.optJSONObject("Error");
      if (error != null) {
        throw new StartException(ERR_API,
                                 error.optString("Code", "Unknown") + "：" +
                                     error.optString("Message", "无错误描述"));
      }
      return response;
    } catch (JSONException e) {
      throw new StartException(ERR_API,
                               "响应 JSON 解析失败：" + truncate(body));
    }
  }

  /** 从 CreateVoiceConversation 的 Response 中提取 TaskId。 */
  private static String parseTaskId(JSONObject response, String action)
      throws StartException {
    String taskId = response.optString("TaskId", "");
    if (isBlank(taskId)) {
      throw new StartException(ERR_API, "响应缺少 TaskId：" +
                                            truncate(response.toString()));
    }
    Log.i(TAG, action + " 成功，TaskId=" + taskId +
                   "，RequestId=" + response.optString("RequestId", ""));
    return taskId;
  }

  private static String truncate(String text) {
    if (text == null)
      return "";
    return text.length() <= 300 ? text : text.substring(0, 300) + "…";
  }

  private static boolean isBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /** 内部执行失败的载体，errorCode 见 ERR_* 常量；对外只通过 Callback 暴露。 */
  private static final class StartException extends Exception {
    final int errorCode;

    StartException(int errorCode, String message) {
      super(message);
      this.errorCode = errorCode;
    }
  }
}
