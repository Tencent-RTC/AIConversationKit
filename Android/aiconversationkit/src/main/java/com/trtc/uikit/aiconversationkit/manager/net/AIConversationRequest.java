package com.trtc.uikit.aiconversationkit.manager.net;

import com.trtc.uikit.aiconversationkit.AIConversationDefine;

public interface AIConversationRequest {
    void startConversation(AIConversationDefine.StartAIConversationParams params);

    void stopConversation();
}
