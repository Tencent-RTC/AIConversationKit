package com.trtc.uikit.aiconversationkit.manager.net;

import android.os.Bundle;
import android.util.Log;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.AIConversationDefine;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;

import java.util.HashMap;
import java.util.Map;

public class ServerAIConversationRequest implements AIConversationRequest {
    private static final String TAG = "ServerAIConversationReq";

    @Override
    public void startConversation(AIConversationDefine.StartAIConversationParams params) {
        Map<String, Object> param = new HashMap<>(3);
        param.put("roomId", params.roomId);
        param.put("roomIdType", ConversationConstant.ROOM_TYPE_STRING);
        param.put("language", params.sttConfig.language);
        Log.d(TAG, String.format("startConversation roomId=%s language=%s", params.roomId, params.sttConfig.language));
        TUICore.callService("AIConversationService",
                "methodStartAIConversation", param, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        if (code != 0) {
                            Log.e(TAG, String.format("startConversation code=%d message==%s", code, message));
                            return;
                        }
                        ConversationState state = ConversationManager.sharedInstance().getConversationState();
                        state.aiRobotUserId = bundle.getString("robotUserId");
                        state.conversationTaskId = bundle.getString("taskId");
                        Log.d(TAG, String.format("startConversation code=%d taskId==%s",
                                code, state.conversationTaskId));
                    }
                });
    }

    @Override
    public void stopConversation() {
        ConversationState state = ConversationManager.sharedInstance().getConversationState();
        Map<String, Object> param = new HashMap<>(2);
        param.put("roomId", state.strRoomId);
        param.put("roomIdType", ConversationConstant.ROOM_TYPE_STRING);
        param.put("taskId", state.conversationTaskId);
        Log.d(TAG, String.format("stopConversation roomId=%s taskId=%s", state.strRoomId, state.conversationTaskId));
        TUICore.callService("AIConversationService",
                "methodStopAIConversation", param, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        Log.d(TAG, String.format("stopConversation code=%d message==%s", code, message));
                    }
                });
    }
}
