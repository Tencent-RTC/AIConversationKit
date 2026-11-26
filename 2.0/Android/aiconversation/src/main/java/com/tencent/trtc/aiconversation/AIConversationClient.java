// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation SDK - 唯一对外入口（单例）
//
// 只负责 TRTC 侧的编排：进房 → 状态/字幕回调 → interrupt → 退房。进房成功后
// 自动订阅字幕流，业务侧无需调用 enableSubtitle()（重连等场景仍可手动重订阅）。
// AI 对话任务的创建与停止由业务侧调用云 API 完成，凭证通过 start(StartParams)
// 传入。
//
// 线程：本类内部不做任何线程切换。所有方法必须在主线程调用，回调也在主线程触发
// （TRTCCloud 不设置 listenerHandler 时默认用主线程驱动回调）。
// 本类不提供超时机制：若 SDK 迟迟不回调，会话会停留在连接中，
// 需要超时处理的调用方可在调用 start() 后自行计时并调用 stop()。
//
// 生命周期：TRTCCloud 单例随本 Client 常驻进程；stop() 退房并回到 IDLE，
// 可再次 start。注意 AI 任务需由业务自行停止。

package com.tencent.trtc.aiconversation;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import com.tencent.rtmp.ui.TXCloudVideoView;
import com.tencent.trtc.TRTCCloud;
import com.tencent.trtc.TRTCCloudDef;
import com.tencent.trtc.TRTCCloudListener;
import com.tencent.trtc.aiconversation.AIConversationDef.AgentState;
import com.tencent.trtc.aiconversation.AIConversationDef.SessionState;
import com.tencent.trtc.aiconversation.AIConversationDef.StartParams;
import com.tencent.trtc.aiconversation.AIConversationDef.SubtitleItem;
import com.tencent.trtc.aiconversation.AIConversationEventParser.AIConversationEvent;
import java.io.ByteArrayOutputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class AIConversationClient {

  // ------------------------------------------------------------------
  // 常量
  // ------------------------------------------------------------------

  private static final String TAG = "AIConversationClient";

  /** 对外 API 日志统一前缀，便于用 logcat 按关键字过滤调用链。 */
  private static final String API_IN = "API --> ";
  private static final String API_OUT = "API <-- ";

  private static final String EVENT_NAME_AI_CONVERSATION =
      "onAIConversationEvent";
  private static final String API_AI_CONVERSATION = "aiConversation";
  /** 服务端协议名，作用是订阅机器人字幕，并非建立对话连接。 */
  private static final String COMMAND_CONNECT = "connect";
  private static final String COMMAND_INTERRUPT = "interrupt";
  /** 视觉理解：把一帧画面（JPEG + Base64）上行给 AI，见文档 140387。 */
  private static final String COMMAND_SEND_IMAGE = "sendImage";
  private static final String IMAGE_FORMAT_JPEG = "jpeg";
  /** 文档示例的 JPEG 压缩质量。 */
  private static final int IMAGE_JPEG_QUALITY = 80;

  /** 进房前下发的音频场景配置：setAudioQualityEx 实验性接口及其两个参数值。 */
  private static final String API_SET_AUDIO_QUALITY_EX = "setAudioQualityEx";
  private static final int AUDIO_QUALITY_SYSTEM_VOLUME_TYPE = 1;
  private static final int AUDIO_QUALITY_AUDIO_SCENE = 64;

  /** 声纹降噪：enableVoiceprintDenoise 实验性接口。 */
  private static final String API_ENABLE_VOICEPRINT_DENOISE =
      "enableVoiceprintDenoise";
  /** 传统自动噪声抑制：enableAudioANS 实验性接口。 */
  private static final String API_ENABLE_AUDIO_ANS = "enableAudioANS";

  /** 进房前下发的网络/音频私有配置：setPrivateConfig 实验性接口及其配置项。 */
  private static final String API_SET_PRIVATE_CONFIG = "setPrivateConfig";
  private static final String CONFIG_KEY_BANDWIDTH_SAVING_MODE =
      "Liteav.Audio.common.bandwidth.saving.mode";
  private static final String CONFIG_KEY_EOS_PACKET_IN_DTX =
      "Liteav.Audio.common.enable.send.eos.packet.in.dtx";
  private static final String CONFIG_KEY_AUDIO_MIN_CACHE =
      "Liteav.Network.audio.min.cache.anchor.v2";
  private static final String CONFIG_KEY_AUDIO_MAX_CACHE =
      "Liteav.Network.audio.max.cache.anchor.v2";
  private static final String PRIVATE_CONFIG_DEFAULT = "0";
  private static final String PRIVATE_CONFIG_VALUE = "0";
  private static final String AUDIO_MIN_CACHE_MS = "100";
  private static final String AUDIO_MAX_CACHE_MS = "300";

  /** AI 回声消除模型类型（setPrivateConfig）。 */
  private static final String CONFIG_KEY_AI_EC_MODEL_TYPE =
      "Liteav.Audio.common.ai.ec.model.type";
  private static final String AI_EC_MODEL_TYPE_VALUE = "7";
  private static final String AI_EC_MODEL_TYPE_DEFAULT = "4";
  /** 关闭麦克风 SNR 选路（setPrivateConfig）。 */
  private static final String CONFIG_KEY_DISABLE_MIC_SNR_SELECTION =
      "Liteav.Audio.common.disable.microphone.snr.selection";
  private static final String DISABLE_MIC_SNR_SELECTION_VALUE = "1024";
  private static final String DISABLE_MIC_SNR_SELECTION_DEFAULT = "1";

  // ------------------------------------------------------------------
  // 单例与字段
  // ------------------------------------------------------------------

  private static volatile AIConversationClient instance;

  private final Context appContext;
  private AIConversationListener listener;
  private TRTCCloud trtc;

  private volatile SessionState sessionState = SessionState.IDLE;
  private volatile AgentState agentState = AgentState.IDLE;

  private String robotId;

  /**
   * 已主动调用 exitRoom()、正等 SDK 的 onExitRoom 确认。该回调异步到达时
   * 可能已经 start() 了新会话，不能靠 isInRoom() 判断它属于哪一届。
   */
  private boolean expectingExitRoom = false;

  /** 本地摄像头预览是否开启。预览独立于会话，由调用方负责在界面销毁时关闭。 */
  private boolean localPreviewing = false;

  /** 当前是否使用前置摄像头，每次开启预览都从后置开始。 */
  private boolean frontCamera = false;

  public static AIConversationClient getInstance(Context context) {
    Log.i(TAG, API_IN + "getInstance(context=" +
                   (context == null ? "null" : "present") + ")");
    AIConversationClient result = instance;
    if (result == null) {
      synchronized (AIConversationClient.class) {
        result = instance;
        if (result == null) {
          result = new AIConversationClient(context);
          instance = result;
          Log.i(TAG, "AIConversationClient instance created");
        }
      }
    }
    Log.i(TAG, API_OUT + "getInstance cached=" + (instance != null));
    return result;
  }

  private AIConversationClient(Context context) {
    this.appContext = context.getApplicationContext();
  }

  // ------------------------------------------------------------------
  // 对外 API
  // ------------------------------------------------------------------

  /**
   * 设置事件监听器，同一时刻只有一个，后设置的会覆盖前者。传 null
   * 表示不再接收回调。
   *
   * <p>本类是单例，监听器跨页面共享；页面不再需要回调时必须调用
   * setListener(null)， 否则会继续收到回调并持有该页面。
   */
  public void setListener(AIConversationListener listener) {
    Log.i(TAG, API_IN + "setListener(" + typeOf(listener) + ")");
    this.listener = listener;
  }

  /**
   * 进房并启动 AI 对话。进房结果通过 onConversationStarted 的 errorCode 表达：
   * CODE_OK 表示 AI 对话已可用，否则为进房错误码。仅在会话空闲时可调用。
   *
   * <p>参数或状态不合法时不产生任何回调，直接返回错误码。
   *
   * <p>进房成功后自动订阅字幕流，业务侧一般无需调用 enableSubtitle()；仅在
   * 重连等需要重新订阅的场景手动调用即可。订阅失败只影响 onSubtitle 回调，
   * 不影响 ASR / LLM / TTS 等对话能力。
   *
   * @param startParams 进房凭证与房间号
   * @param robotId     AI 机器人的 UserId，即创建任务时的 AgentConfig.UserId，
   *                    用于字幕订阅指令以及过滤本机器人的回调
   * @return AIConversationError.CODE_OK 表示已发起进房；否则为 CODE_* 错误码
   */
  public int start(StartParams startParams, String robotId) {
    Log.i(TAG, API_IN + "start(" + describeStartParams(startParams) +
                   ", robotId=" + robotId + ", state=" + sessionState + ")");
    if (isInRoom()) {
      return logApiResult("start", AIConversationError.CODE_ILLEGAL_STATE,
                          "already in room");
    }
    if (startParams == null || !startParams.isValid() ||
        isNullOrBlank(robotId)) {
      return logApiResult("start", AIConversationError.CODE_INVALID_PARAM,
                          "startParams=" + describeStartParams(startParams) +
                              ", robotId=" + robotId);
    }

    this.robotId = robotId;
    this.sessionState = SessionState.CONNECTING;
    enterRoom(startParams);
    return logApiResult("start", AIConversationError.CODE_OK, "entering room");
  }

  /**
   * 退房并结束会话。幂等，可在任意状态调用。
   * 注意：AI 对话任务需由业务侧调用自己的后端停止，本方法不负责。
   */
  public void stop() {
    Log.i(TAG,
          API_IN + "stop(state=" + sessionState + ", robotId=" + robotId + ")");
    if (!isInRoom()) {
      Log.i(TAG, API_OUT + "stop ignored: not in room");
      return;
    }
    // 主动停止，需要退房。
    endSession(true);
    Log.i(TAG, API_OUT + "stop done, state=" + sessionState);
  }

  /**
   * 打断 AI 当前播报。仅 READY 状态有效；状态不合法时不产生任何回调，直接
   * 返回错误码。返回 CODE_OK 只表示指令已发出，不代表服务端执行成功。
   *
   * @return AIConversationError.CODE_OK 表示已发出打断指令；否则为 CODE_*
   *     错误码
   */
  public int interrupt() {
    Log.i(TAG, API_IN + "interrupt(state=" + sessionState +
                   ", robotId=" + robotId + ")");
    if (sessionState != SessionState.READY || this.robotId == null) {
      return logApiResult("interrupt", AIConversationError.CODE_ILLEGAL_STATE,
                          "requires READY");
    }
    sendAIConversationCommand(COMMAND_INTERRUPT, this.robotId);
    return logApiResult("interrupt", AIConversationError.CODE_OK,
                        "command sent");
  }

  /**
   * 订阅 AI 机器人的字幕流。不调用它仅仅收不到 onSubtitle，ASR / LLM / TTS
   * 等对话能力不受影响。 建议在 onConversationStarted()
   * 回调之后调用；可在会话进行中重复调用
   * （例如重连后重新订阅）。状态不合法时不产生任何回调，直接返回错误码；
   * 返回 CODE_OK 只表示指令已发出，不代表服务端订阅成功。
   *
   * @return AIConversationError.CODE_OK 表示已发出订阅指令；否则为 CODE_*
   *     错误码
   */
  public int enableSubtitle() {
    Log.i(TAG, API_IN + "enableSubtitle(state=" + sessionState +
                   ", robotId=" + robotId + ")");
    if (!isInRoom() || this.robotId == null) {
      return logApiResult("enableSubtitle",
                          AIConversationError.CODE_ILLEGAL_STATE,
                          "requires an active session");
    }
    sendAIConversationCommand(COMMAND_CONNECT, this.robotId);
    return logApiResult("enableSubtitle", AIConversationError.CODE_OK,
                        "command sent");
  }

  /**
   * 静音/取消静音本地音频。仅在会话进行中有效；状态不合法时不产生任何回调，
   * 直接返回错误码。
   *
   * @return AIConversationError.CODE_OK 表示已设置；否则为 CODE_* 错误码
   */
  public int muteLocalAudio(boolean muted) {
    Log.i(TAG, API_IN + "muteLocalAudio(muted=" + muted +
                   ", state=" + sessionState + ")");
    if (!isInRoom()) {
      return logApiResult("muteLocalAudio",
                          AIConversationError.CODE_ILLEGAL_STATE,
                          "not in room");
    }
    getTrtcCloud().muteLocalAudio(muted);
    return logApiResult("muteLocalAudio", AIConversationError.CODE_OK,
                        "applied");
  }

  /**
   * 开启本地摄像头预览（前置）。画面只在本地显示，不向房间推送视频流。
   * 调用方需先取得 CAMERA 权限，并保证 view 在预览期间一直挂载在界面上。
   * 重复调用会以新的 view 重新开启预览。
   *
   * @return AIConversationError.CODE_OK 表示已开启；view 为空时返回
   *     CODE_INVALID_PARAM
   */
  public int startLocalPreview(TXCloudVideoView view) {
    Log.i(TAG, API_IN + "startLocalPreview(view=" +
                   (view == null ? "null" : "present") + ")");
    if (view == null) {
      return logApiResult("startLocalPreview",
                          AIConversationError.CODE_INVALID_PARAM,
                          "view is null");
    }
    TRTCCloud cloud = getTrtcCloud();
    cloud.muteLocalVideo(TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG, true);
    frontCamera = false;
    cloud.startLocalPreview(frontCamera, view);
    localPreviewing = true;
    return logApiResult("startLocalPreview", AIConversationError.CODE_OK,
                        "preview started");
  }

  /**
   * 切换前后摄像头，仅在预览开启时有效。
   *
   * @return 切换后是否为前置摄像头；预览未开启时保持原状态不变
   */
  public boolean switchCamera() {
    Log.i(TAG, API_IN + "switchCamera(previewing=" + localPreviewing + ")");
    if (!localPreviewing) {
      return frontCamera;
    }
    frontCamera = !frontCamera;
    getTrtcCloud().getDeviceManager().switchCamera(frontCamera);
    return frontCamera;
  }

  /**
   * 截取当前本地摄像头画面并发送给 AI，AI 会结合用户随后的语音提问理解画面。
   * 需要会话处于 READY 且本地预览已开启；创建任务时必须启用 VisionConfig，
   * 且 LLM 必须是多模态模型，否则画面不会被理解。
   *
   * <p>截图是异步的。发送成功没有回调，失败时服务端经 onCommand
   * 事件返回错误码。 返回 CODE_OK 只表示已开始截图，不代表图片已送达。
   *
   * @return AIConversationError.CODE_OK 表示已开始截图；否则为 CODE_* 错误码
   */
  public int sendVideoSnapshot() {
    Log.i(TAG, API_IN + "sendVideoSnapshot(state=" + sessionState +
                   ", previewing=" + localPreviewing +
                   ", robotId=" + robotId + ")");
    if (sessionState != SessionState.READY || this.robotId == null) {
      return logApiResult("sendVideoSnapshot",
                          AIConversationError.CODE_ILLEGAL_STATE,
                          "requires READY");
    }
    if (!localPreviewing) {
      return logApiResult("sendVideoSnapshot",
                          AIConversationError.CODE_ILLEGAL_STATE,
                          "local preview is not started");
    }
    // 截图回调是异步的，期间会话可能已结束或换了机器人，所以发送时用截图瞬间的
    // robotId。
    final String targetRobotId = this.robotId;
    getTrtcCloud().snapshotVideo("", TRTCCloudDef.TRTC_VIDEO_STREAM_TYPE_BIG,
                                 TRTCCloudDef.TRTC_SNAPSHOT_SOURCE_TYPE_VIEW,
                                 new TRTCCloudListener.TRTCSnapshotListener() {
                                   @Override
                                   public void onSnapshotComplete(
                                       Bitmap bitmap) {
                                     onSnapshotCaptured(bitmap, targetRobotId);
                                   }
                                 });
    return logApiResult("sendVideoSnapshot", AIConversationError.CODE_OK,
                        "snapshot requested");
  }

  private void onSnapshotCaptured(Bitmap bitmap, String targetRobotId) {
    if (bitmap == null) {
      Log.w(TAG, "snapshotVideo returned null bitmap");
      return;
    }
    // 会话已结束或已切换到别的机器人，这张图不应再发给旧会话。
    if (sessionState != SessionState.READY ||
        !targetRobotId.equals(this.robotId)) {
      Log.w(TAG, "snapshot dropped: session changed, state=" + sessionState);
      return;
    }
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      bitmap.compress(Bitmap.CompressFormat.JPEG, IMAGE_JPEG_QUALITY, out);
      JSONObject payload = new JSONObject();
      payload.put("format", IMAGE_FORMAT_JPEG);
      payload.put("image",
                  Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP));
      sendAIConversationCommand(COMMAND_SEND_IMAGE, targetRobotId, payload);
      Log.i(TAG, "snapshot sent, jpeg bytes=" + out.size() + ", size=" +
                     bitmap.getWidth() + "x" + bitmap.getHeight());
    } catch (JSONException e) {
      Log.e(TAG, "failed to build sendImage payload", e);
    }
  }

  /** 本地摄像头预览是否正在进行。 */
  public boolean isLocalPreviewing() { return localPreviewing; }

  /** 关闭本地摄像头预览。幂等；不随会话结束自动关闭，需由调用方显式调用。 */
  public void stopLocalPreview() {
    Log.i(TAG, API_IN + "stopLocalPreview(previewing=" + localPreviewing + ")");
    if (!localPreviewing) {
      return;
    }
    localPreviewing = false;
    getTrtcCloud().stopLocalPreview();
  }

  // ------------------------------------------------------------------
  // 会话状态
  // ------------------------------------------------------------------

  /** 是否有进行中的会话（已进房且尚未退房）。 */
  private boolean isInRoom() {
    return this.sessionState == SessionState.CONNECTING ||
        this.sessionState == SessionState.READY;
  }

  /** 是否正在等进房结果。 */
  private boolean isEnteringRoom() {
    return this.sessionState == SessionState.CONNECTING;
  }

  /** 该 robotId 是否属于当前进行中的会话，用于过滤迟到的、其他机器人的事件。 */
  private boolean isActiveRobot(String robotId) {
    return robotId != null && robotId.equals(this.robotId) && isInRoom();
  }

  /** 更新机器人状态；仅在值变化时通知 UI。 */
  private void updateAgentState(AgentState state) {
    if (agentState == state)
      return;
    agentState = state;
    notifyAgentState(state);
  }

  /** 致命错误：先通知错误，再结束会话。 */
  private void failSession(AIConversationError error, boolean needExitRoom) {
    notifyError(error);
    endSession(needExitRoom);
  }

  /**
   * 结束本次会话：重置内部状态、按需退房，回到 IDLE 并通知 UI。
   *
   * <p>幂等：状态已不是进行中时直接返回。listener 可能在 onError 回调里调用
   * stop()，没有这层保护会嵌套执行两次，导致重复退房和重复
   * onConversationStopped。
   *
   * @param needExitRoom 是否需要退房；被踢出房间等被动场景下房间已经没了，传
   *     false
   *
   * <p>agentState 只做静默重置，不再回调 UI：会话终态本身已表达"结束"。
   * 主动退房时置 expectingExitRoom，由它认领随后异步到达的 onExitRoom。
   */
  private void endSession(boolean needExitRoom) {
    if (!isInRoom())
      return;
    this.robotId = null;
    agentState = AgentState.IDLE;

    if (needExitRoom) {
      TRTCCloud cloud = getTrtcCloud();
      expectingExitRoom = true;
      cloud.stopLocalAudio();
      cloud.exitRoom();
    }
    sessionState = SessionState.IDLE;
    notifyConversationStopped();
  }

  // ------------------------------------------------------------------
  // TRTC 调用
  // ------------------------------------------------------------------

  /** 首次 start 时创建 TRTCCloud 并注册监听，之后复用。 */
  private TRTCCloud getTrtcCloud() {
    if (trtc == null) {
      trtc = TRTCCloud.sharedInstance(appContext);
      trtc.addListener(trtcListener);
    }
    return trtc;
  }

  private void enterRoom(StartParams startParams) {
    TRTCCloud cloud = getTrtcCloud();
    applyAIConversationAudioConfig(cloud);
    applyAiConversationNetworkConfig(cloud);
    TRTCCloudDef.TRTCParams roomParams = new TRTCCloudDef.TRTCParams();
    roomParams.sdkAppId = startParams.sdkAppId;
    roomParams.userId = startParams.userId;
    roomParams.userSig = startParams.userSig;
    roomParams.strRoomId = startParams.roomId;
    roomParams.role = TRTCCloudDef.TRTCRoleAnchor;
    cloud.enterRoom(roomParams, TRTCCloudDef.TRTC_APP_SCENE_VIDEOCALL);
  }

  private void applyAIConversationAudioConfig(TRTCCloud cloud) {
    try {
      JSONObject params = new JSONObject();
      params.put("systemVolumeType", AUDIO_QUALITY_SYSTEM_VOLUME_TYPE);
      params.put("audioScene", AUDIO_QUALITY_AUDIO_SCENE);
      JSONObject request = new JSONObject();
      request.put("api", API_SET_AUDIO_QUALITY_EX);
      request.put("params", params);
      cloud.callExperimentalAPI(request.toString());
      Log.i(TAG, "callExperimentalAPI " + request);

      callSetPrivateConfig(cloud, privateConfigItem(CONFIG_KEY_AI_EC_MODEL_TYPE,
                                                    AI_EC_MODEL_TYPE_VALUE,
                                                    AI_EC_MODEL_TYPE_DEFAULT));
      callSetPrivateConfig(
          cloud, privateConfigItem(CONFIG_KEY_DISABLE_MIC_SNR_SELECTION,
                                   DISABLE_MIC_SNR_SELECTION_VALUE,
                                   DISABLE_MIC_SNR_SELECTION_DEFAULT));
    } catch (JSONException e) {
      Log.e(TAG, "failed to build audio config request", e);
    }
  }

  private void applyAiConversationNetworkConfig(TRTCCloud cloud) {
    try {
      callSetPrivateConfig(cloud,
                           privateConfigItem(CONFIG_KEY_BANDWIDTH_SAVING_MODE,
                                             PRIVATE_CONFIG_VALUE));
      callSetPrivateConfig(cloud,
                           privateConfigItem(CONFIG_KEY_EOS_PACKET_IN_DTX,
                                             PRIVATE_CONFIG_VALUE));
      callSetPrivateConfig(cloud, privateConfigItem(CONFIG_KEY_AUDIO_MIN_CACHE,
                                                    AUDIO_MIN_CACHE_MS));
      callSetPrivateConfig(cloud, privateConfigItem(CONFIG_KEY_AUDIO_MAX_CACHE,
                                                    AUDIO_MAX_CACHE_MS));
    } catch (JSONException e) {
      Log.e(TAG, "failed to build setPrivateConfig request", e);
    }
  }

  private static JSONObject privateConfigItem(String key, String value)
      throws JSONException {
    return privateConfigItem(key, value, PRIVATE_CONFIG_DEFAULT);
  }

  private static JSONObject privateConfigItem(String key, String value,
                                              String defaultValue)
      throws JSONException {
    JSONObject item = new JSONObject();
    item.put("default", defaultValue);
    item.put("key", key);
    item.put("value", value);
    return item;
  }

  private static void callSetPrivateConfig(TRTCCloud cloud, JSONObject item)
      throws JSONException {
    JSONArray configs = new JSONArray();
    configs.put(item);
    JSONObject params = new JSONObject();
    params.put("configs", configs);
    JSONObject request = new JSONObject();
    request.put("api", API_SET_PRIVATE_CONFIG);
    request.put("params", params);
    cloud.callExperimentalAPI(request.toString());
    Log.i(TAG, "callExperimentalAPI " + request);
  }

  private void sendAIConversationCommand(String command, String botId) {
    sendAIConversationCommand(command, botId, null);
  }

  private void sendAIConversationCommand(String command, String botId,
                                         JSONObject payload) {
    TRTCCloud cloud = getTrtcCloud();
    try {
      JSONObject paramsJson = new JSONObject();
      paramsJson.put("command", command);
      paramsJson.put("robotid", botId);
      if (payload != null) {
        paramsJson.put("payload", payload);
      }
      JSONObject request = new JSONObject();
      request.put("api", API_AI_CONVERSATION);
      request.put("params", paramsJson);
      cloud.callExperimentalAPI(request.toString());
    } catch (JSONException e) {
      Log.e(TAG, "failed to build aiConversation request", e);
    }
  }

  private void onEnterRoomResult(long result) {
    if (!isEnteringRoom())
      return;

    if (result <= 0) {
      notifyConversationStarted((int)result, "enter room failed: " + result);
      return;
    }

    getTrtcCloud().startLocalAudio(TRTCCloudDef.TRTC_AUDIO_QUALITY_SPEECH);
    sessionState = SessionState.READY;

    int code = enableSubtitle();
    if (code != AIConversationError.CODE_OK) {
      Log.w(TAG, "auto enableSubtitle failed: " + code);
    }
    notifyConversationStarted(AIConversationError.CODE_OK, "");
  }

  private void onCommandEvent(AIConversationEvent.CommandEvent event) {
    if (!isActiveRobot(event.getRobotId()))
      return;
    Log.d(TAG, "onCommand: " + event.getCommand() +
                   ", errCode=" + event.getErrCode());
  }

  // ------------------------------------------------------------------
  // TRTC 回调
  // ------------------------------------------------------------------

  private final TRTCCloudListener trtcListener = new TRTCCloudListener() {
    @Override
    public void onEnterRoom(long result) {
      onEnterRoomResult(result);
    }

    @Override
    public void onExitRoom(int reason) {
      // exitRoom() 的确认回调。它可能晚于新的 start()，届时 isInRoom() 已重新
      // 为 true，不能用状态判断归属；用显式标志认领并忽略，只认领一次。
      if (expectingExitRoom) {
        expectingExitRoom = false;
        return;
      }
      if (!isInRoom())
        return;
      // 非主动退房（被踢、房间解散等）：房间已经没了，无需再调 exitRoom。
      failSession(new AIConversationError(AIConversationError.CODE_ROOM_EXITED,
                                          "exit room, reason=" + reason),
                  false);
    }

    @Override
    public void onError(int code, String errMsg, Bundle extraInfo) {
      String message = errMsg != null ? errMsg : "";
      // 进房失败时 SDK 会同时触发 onEnterRoom(<0) 和 onError，由 onEnterRoom
      // 统一处理。
      if (isEnteringRoom()) {
        Log.w(TAG, "trtc error while entering room: " + code + " " + message);
        return;
      }
      if (isInRoom()) {
        failSession(new AIConversationError(code, message), true);
        return;
      }
      Log.w(TAG, "trtc error ignored in " + sessionState + ": " + code + " " +
                     message);
    }

    @Override
    public void onExperimentalEvent(String eventName, String paramsJson) {
      if (!EVENT_NAME_AI_CONVERSATION.equals(eventName) || paramsJson == null ||
          paramsJson.isEmpty()) {
        Log.d(TAG, "RAW ignored: eventName mismatch or empty params");
        return;
      }
      AIConversationEvent event = AIConversationEventParser.parse(paramsJson);
      if (event == null) {
        Log.w(TAG, "RAW parse returned null, check the payload logged above");
        return;
      }
      Log.d(TAG, "RAW parsed -> " + event.getClass().getSimpleName());
      if (event instanceof AIConversationEvent.CommandEvent) {
        onCommandEvent((AIConversationEvent.CommandEvent)event);
      } else if (event instanceof AIConversationEvent.AgentEvent) {
        AIConversationEvent.AgentEvent e =
            (AIConversationEvent.AgentEvent)event;
        if (isActiveRobot(e.getRobotId()))
          updateAgentState(e.getState());
      } else if (event instanceof AIConversationEvent.SubtitleEvent) {
        AIConversationEvent.SubtitleEvent e =
            (AIConversationEvent.SubtitleEvent)event;
        if (isActiveRobot(e.getRobotId())) {
          notifySubtitle(e.getItem());
        } else {
          // 记录被过滤的字幕，便于区分「SDK 没发字幕」与「robotId /
          // 状态不匹配」
          Log.d(TAG, "SUBTITLE ignored: robotId=" + e.getRobotId() +
                         ", current=" + AIConversationClient.this.robotId +
                         ", state=" + AIConversationClient.this.sessionState);
        }
      }
    }
  };

  // ------------------------------------------------------------------
  // 事件派发
  // ------------------------------------------------------------------

  private void notifyConversationStarted(final int errorCode,
                                         final String errorMessage) {
    dispatch(new ListenerAction() {
      @Override
      public void run(AIConversationListener listener) {
        listener.onConversationStarted(errorCode, errorMessage);
      }
    });
  }

  private void notifyConversationStopped() {
    dispatch(new ListenerAction() {
      @Override
      public void run(AIConversationListener listener) {
        listener.onConversationStopped();
      }
    });
  }

  private void notifyAgentState(final AgentState state) {
    dispatch(new ListenerAction() {
      @Override
      public void run(AIConversationListener listener) {
        listener.onAgentStateChanged(state);
      }
    });
  }

  private void notifyError(final AIConversationError error) {
    Log.w(TAG, "error: " + error);
    dispatch(new ListenerAction() {
      @Override
      public void run(AIConversationListener listener) {
        listener.onError(error);
      }
    });
  }

  private void notifySubtitle(final SubtitleItem item) {
    // 同一轮次会流式回调多次（text 是截至当前的完整文本），流式过程用 debug，
    // 句子定稿用 info，避免刷屏又能在常规日志里看到完整对话。
    if (item != null) {
      String line = "SUBTITLE role=" + item.role + ", roundId=" + item.roundId +
                    ", end=" + item.isEnd + ", taskId=" + item.taskId +
                    ", ts=" + item.timestampMs + ", text=" + item.text;
      if (item.isEnd) {
        Log.i(TAG, line);
      } else {
        Log.d(TAG, line);
      }
    }
    dispatch(new ListenerAction() {
      @Override
      public void run(AIConversationListener listener) {
        listener.onSubtitle(item);
      }
    });
  }

  private interface ListenerAction {
    void run(AIConversationListener listener);
  }

  /** 派发给当前监听器，监听器抛出的异常只记录日志，不影响状态机。 */
  private void dispatch(ListenerAction action) {
    AIConversationListener current = listener;
    if (current == null)
      return;
    try {
      action.run(current);
    } catch (Exception e) {
      Log.e(TAG, "listener threw", e);
    }
  }

  // ------------------------------------------------------------------
  // 工具
  // ------------------------------------------------------------------

  public static boolean isNullOrBlank(String value) {
    return value == null || value.trim().isEmpty();
  }

  /**
   * 打印 API 返回值并原样返回，方便在每个 return 分支记录实际结果。
   * 用返回 int 的形式串联，避免改变原有控制流。
   */
  private static int logApiResult(String api, int code, String detail) {
    Log.i(TAG, API_OUT + api + " result=" + codeName(code) + " (" + code +
                   "), " + detail);
    return code;
  }

  /**
   * 只打印进房参数的非敏感字段。userSig 是登录凭证，任何情况下都不能写日志。
   */
  private static String describeStartParams(StartParams params) {
    if (params == null) {
      return "startParams{null}";
    }
    return "startParams{sdkAppId=" + params.sdkAppId +
        ", userId=" + params.userId + ", roomId=" + params.roomId +
        ", userSig=" + (isNullOrBlank(params.userSig) ? "empty" : "present") +
        ", valid=" + params.isValid() + "}";
  }

  private static String typeOf(Object value) {
    if (value == null) {
      return "null";
    }
    return value.getClass().getSimpleName();
  }

  /** 把 SDK 自定义错误码转成可读名字；透传的底层错误码保留数字。 */
  private static String codeName(int code) {
    switch (code) {
    case AIConversationError.CODE_OK:
      return "OK";
    case AIConversationError.CODE_ILLEGAL_STATE:
      return "ILLEGAL_STATE";
    case AIConversationError.CODE_INVALID_PARAM:
      return "INVALID_PARAM";
    case AIConversationError.CODE_ROOM_EXITED:
      return "ROOM_EXITED";
    default:
      return "SDK_ERR";
    }
  }
}
