// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation SDK - 公共枚举定义
//
// 用法与 TRTCCloudDef 一致：枚举集中在本类中，使用时可
// import com.tencent.trtc.aiconversation.AIConversationDef.SessionState; 后直接写 SessionState。

package com.tencent.trtc.aiconversation;

public final class AIConversationDef {

    private AIConversationDef() {
    }

    /**
     * 进房参数。业务侧调用自己的后端创建 AI 对话任务后，把凭证填入即可。
     *
     * <pre>{@code
     * StartParams params = new StartParams(sdkAppId, userId, userSig, roomId);
     * client.start(params, robotId);
     * }</pre>
     *
     * <p>也可以无参构造后直接给 public 字段赋值。
     * 房间统一使用字符串房间号（官方文档推荐，对应创建任务时的 RoomIdType=1）。
     */
    public static class StartParams {
        public int sdkAppId;
        public String userId;
        public String userSig;
        public String roomId;

        public StartParams() {
        }

        public StartParams(int sdkAppId, String userId, String userSig, String roomId) {
            this.sdkAppId = sdkAppId;
            this.userId = userId;
            this.userSig = userSig;
            this.roomId = roomId;
        }

        /** 各字段是否已填好：sdkAppId 为正，三个字符串均非空白。 */
        public boolean isValid() {
            return sdkAppId > 0
                    && !isEmpty(userId)
                    && !isEmpty(userSig)
                    && !isEmpty(roomId);
        }

        private static boolean isEmpty(String value) {
            return value == null || value.trim().isEmpty();
        }
    }

    /**
     * 一条字幕，字段直接公开，不可变。
     *
     * <p>字段见文档 140338「实时字幕（客户端）」。同一个 {@link #roundId} + {@link #role}
     * 会被多次回调：{@link #text} 是「该句(ASR) / 本轮(LLM) 截至当前的完整文本」，
     * 不是增量片段；{@link #isEnd} 来自 payload 的 end 字段，false 为中间结果、
     * true 表示该句识别完成 / 本轮回复结束。
     * {@link #roundId} 是对话轮次 ID（同一轮的 ASR 与 LLM 字幕相同），
     * {@link #taskId} 是 AI 对话任务 ID（一个任务含多轮），两者不能混用。
     */
    public static class SubtitleItem {

        /** 字幕的说话方。 */
        public static enum Role {
            /** 用户（ASR 识别结果）。 */
            USER,

            /** AI 机器人（LLM 回复文本）。 */
            AGENT
        }

        public final String roundId;
        public final Role role;
        public final String text;
        public final boolean isEnd;
        public final String taskId;
        /** 毫秒时间戳。用户字幕取 ASR 的 start_utc_ms；机器人字幕取本地接收时间。 */
        public final long timestampMs;

        public SubtitleItem(String roundId, Role role, String text, boolean isEnd,
                            String taskId, long timestampMs) {
            this.roundId = roundId;
            this.role = role;
            this.text = text;
            this.isEnd = isEnd;
            this.taskId = taskId;
            this.timestampMs = timestampMs;
        }

        @Override
        public String toString() {
            return "SubtitleItem{roundId='" + roundId + "', role=" + role
                    + ", text='" + text + "', isEnd=" + isEnd + "}";
        }
    }

    /**
     * 会话生命周期（内部使用，不对外暴露）。失败后直接回到 IDLE，不单独设状态。
     */
    public static enum SessionState {
        /** 空闲，可调用 start。 */
        IDLE,

        /** 正在进房；进房成功即进入 READY。 */
        CONNECTING,

        /** 连接成功，可以对话。 */
        READY
    }

    /** AI 机器人状态（agentState 消息的 state.code）。 */
    public static enum AgentState {
        IDLE(0),
        LISTENING(1),
        THINKING(2),
        SPEAKING(3),
        INTERRUPTED(4),
        FINISHED(5),
        UNKNOWN(-1);

        private final int code;

        AgentState(int code) {
            this.code = code;
        }

        public int getCode() {
            return code;
        }

        public static AgentState fromCode(int code) {
            for (AgentState state : values()) {
                if (state.code == code) return state;
            }
            return UNKNOWN;
        }
    }
}
