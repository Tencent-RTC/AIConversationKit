package com.trtc.uikit.aiconversationkit.manager.net;

import android.os.Bundle;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.store.AIConversationConfig;
import com.trtc.uikit.aiconversationkit.common.Logger;
import com.trtc.uikit.aiconversationkit.store.AIConversationStoreImpl;
import com.trtc.uikit.aiconversationkit.view.conversation.ConversationConstant;

import java.util.HashMap;
import java.util.Map;

public class ServerAIConversationRequest implements AIConversationRequest {
    private static final String TAG = "ServerAIConversationReq";

    private String mTaskId = "";
    private String mRoomId = "";

    @Override
    public void startConversation(String roomId, AIConversationConfig config) {
        Map<String, Object> param = new HashMap<>(2);
        param.put("roomId", roomId);
        param.put("AIConversationConfig", config);
        Logger.i(TAG, String.format("startConversation roomId=%s", roomId));
        TUICore.callService("AIConversationService",
                "methodStartAIConversation", param, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        if (code != 0) {
                            Logger.e(TAG, String.format("startConversation code:%s message:%s", code, message));
                            return;
                        }
                        String robotId = bundle.getString("robotUserId");
                        AIConversationStoreImpl.Companion.getShared().setAIRobotUserId(robotId);
                        mTaskId = bundle.getString("taskId");
                        mRoomId = roomId;
                        Logger.i(TAG, String.format("startConversation aiRobotUserId:%s taskId:%s", robotId, mTaskId));
                    }
                });
    }

    @Override
    public void stopConversation() {
        Map<String, Object> param = new HashMap<>(2);
        param.put("roomId", mRoomId);
        param.put("roomIdType", ConversationConstant.ROOM_TYPE_STRING);
        param.put("taskId", mTaskId);
        Logger.i(TAG, String.format("stopConversation roomId=%s taskId=%s", mRoomId, mTaskId));
        TUICore.callService("AIConversationService",
                "methodStopAIConversation", param, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        Logger.i(TAG, String.format("stopConversation code=%s message==%s", code, message));
                        mTaskId = "";
                        mRoomId = "";
                    }
                });
    }
}
