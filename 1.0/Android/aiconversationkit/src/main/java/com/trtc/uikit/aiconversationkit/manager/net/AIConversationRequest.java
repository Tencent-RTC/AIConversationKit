package com.trtc.uikit.aiconversationkit.manager.net;

import com.trtc.uikit.aiconversationkit.store.AIConversationConfig;

public interface AIConversationRequest {
    void startConversation(String roomId, AIConversationConfig config);

    void stopConversation();
}
