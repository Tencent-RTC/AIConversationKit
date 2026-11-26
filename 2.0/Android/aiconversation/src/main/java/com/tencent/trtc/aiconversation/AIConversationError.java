// Copyright (c) 2026 Tencent. All rights reserved.
// AI Conversation SDK - 统一错误

package com.tencent.trtc.aiconversation;

/**
 * 统一错误，字段直接公开，不可变。
 *
 * <p>{@link #code} 可能是 SDK 自定义的错误常量，也可能是透传的底层错误码
 * （进房结果、connect / interrupt 的 errCode、TRTC 的 onError 错误码）。
 * {@link #message} 是可用于提示或日志的描述。
 */
public class AIConversationError {

    /** start / interrupt 等同步接口的成功返回码。 */
    public static final int CODE_OK = 0;

    // SDK 自定义错误码用正数，便于与透传的底层错误码（多为负数）区分。
    public static final int CODE_ILLEGAL_STATE = 1001;
    public static final int CODE_INVALID_PARAM = 1004;
    public static final int CODE_ROOM_EXITED = 1005;

    public final int code;
    public final String message;

    public AIConversationError(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public String toString() {
        return "AIConversationError{code=" + code + ", message='" + message + "'}";
    }
}
