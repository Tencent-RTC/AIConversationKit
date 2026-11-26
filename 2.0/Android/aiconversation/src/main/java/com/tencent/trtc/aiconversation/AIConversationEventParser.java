// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation SDK - onAIConversationEvent 消息解析（内部使用）
//
// 协议见官方文档：
//  - onCommand：指令执行结果（connect / interrupt）
//  - agentState (10102)：机器人状态
//  - asrSubtitle (10100) / llmSubtitle (10101)：字幕

package com.tencent.trtc.aiconversation;

import com.tencent.trtc.aiconversation.AIConversationDef.SubtitleItem;
import org.json.JSONException;
import org.json.JSONObject;

final class AIConversationEventParser {

  /** 解析结果。 */
  abstract static class AIConversationEvent {

    /** 指令执行结果（connect / interrupt）。 */
    static final class CommandEvent extends AIConversationEvent {

      private final String command;
      private final String robotId;
      private final int errCode;
      private final String errMsg;

      CommandEvent(String command, String robotId, int errCode, String errMsg) {
        this.command = command;
        this.robotId = robotId;
        this.errCode = errCode;
        this.errMsg = errMsg;
      }

      String getCommand() { return command; }

      String getRobotId() { return robotId; }

      int getErrCode() { return errCode; }

      String getErrMsg() { return errMsg; }
    }

    /** 机器人状态变化。 */
    static final class AgentEvent extends AIConversationEvent {

      private final String robotId;
      private final AIConversationDef.AgentState state;

      AgentEvent(String robotId, AIConversationDef.AgentState state) {
        this.robotId = robotId;
        this.state = state;
      }

      String getRobotId() { return robotId; }

      AIConversationDef.AgentState getState() { return state; }
    }

    /** 字幕。 */
    static final class SubtitleEvent extends AIConversationEvent {

      private final String robotId;
      private final SubtitleItem item;

      SubtitleEvent(String robotId, SubtitleItem item) {
        this.robotId = robotId;
        this.item = item;
      }

      String getRobotId() { return robotId; }

      SubtitleItem getItem() { return item; }
    }
  }

  private static final String EVENT_ON_COMMAND = "onCommand";
  private static final String EVENT_AGENT_STATE = "agentState";
  private static final String EVENT_ASR_SUBTITLE = "asrSubtitle";
  private static final String EVENT_LLM_SUBTITLE = "llmSubtitle";

  private AIConversationEventParser() {}

  /** 解析 params JSON；无法识别或格式错误时返回 null。 */
  static AIConversationEvent parse(String params) {
    try {
      JSONObject root = new JSONObject(params);
      String robotId = root.optString("robotid");
      String event = root.optString("event");
      if (EVENT_ON_COMMAND.equals(event)) {
        return new AIConversationEvent.CommandEvent(
            root.optString("command"), robotId, root.optInt("errCode"),
            root.optString("errMsg"));
      }
      if (EVENT_AGENT_STATE.equals(event)) {
        JSONObject payload =
            root.getJSONObject("data").getJSONObject("payload");
        int code = payload.getJSONObject("state").getInt("code");
        return new AIConversationEvent.AgentEvent(
            robotId, AIConversationDef.AgentState.fromCode(code));
      }
      if (EVENT_ASR_SUBTITLE.equals(event)) {
        return parseSubtitle(root, robotId, SubtitleItem.Role.USER);
      }
      if (EVENT_LLM_SUBTITLE.equals(event)) {
        return parseSubtitle(root, robotId, SubtitleItem.Role.AGENT);
      }
      return null;
    } catch (JSONException e) {
      return null;
    }
  }

  private static AIConversationEvent
  parseSubtitle(JSONObject root, String robotId, SubtitleItem.Role role)
      throws JSONException {
    JSONObject payload = root.getJSONObject("data").getJSONObject("payload");
    long timestamp;
    if (role == SubtitleItem.Role.USER) {
      timestamp = payload.optLong("start_utc_ms", System.currentTimeMillis());
    } else {
      timestamp = System.currentTimeMillis();
    }

    String text = payload.optString("text", "").trim();
    String taskId = payload.optString("taskid", "");
    String roundId = payload.optString("roundid", "");
    boolean isEnd = payload.optBoolean("end", false);
    SubtitleItem item =
        new SubtitleItem(roundId, role, text, isEnd, taskId, timestamp);
    return new AIConversationEvent.SubtitleEvent(robotId, item);
  }
}
