// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation SDK - 事件监听器
//
// 所有方法均有空默认实现，按需重写。所有回调都在主线程触发。

package com.tencent.trtc.aiconversation;

import com.tencent.trtc.aiconversation.AIConversationDef.AgentState;
import com.tencent.trtc.aiconversation.AIConversationDef.SubtitleItem;

public interface AIConversationListener {

  /**
   * 进房成功，AI 对话已可用。
   *
   * @param errorCode    AIConversationError.CODE_OK 表示成功，否则为错误码
   * @param errorMessage 错误描述，成功时为空串
   */
  default void onConversationStarted(int errorCode, String errorMessage) {}

  /**
   * 会话结束。主动 stop，或启动失败、被移出房间等导致会话被清理时触发。
   * 失败场景下会先收到 {@link #onError}。
   */
  default void onConversationStopped() {}

  /** AI 机器人状态变化（空闲 / 聆听 / 思考 / 说话 / 被打断 / 已说完）。 */
  default void onAgentStateChanged(AgentState state) {}

  /** 字幕更新，见 {@link SubtitleItem}。 */
  default void onSubtitle(SubtitleItem item) {}

  /**
   * 错误。致命错误（启动失败、被移出房间等）之后，会紧跟一次
   * {@link #onConversationStopped()}；非致命错误（如 interrupt 失败）会话继续。
   */
  default void onError(AIConversationError error) {}
}
