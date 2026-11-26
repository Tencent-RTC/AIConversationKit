package com.trtc.uikit.aiconversationkit.manager.net;

import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;

import java.util.Map;

public class ClientFeedbackRequest implements FeedbackRequest {
    @Override
    public void uploadFeedback(Map<String, String> param, TUIServiceCallback callback) {
    }

    @Override
    public void fetchFeedback() {
    }

    @Override
    public void fetchRemainingExperienceTime() {
    }

    @Override
    public void deductionExperienceTime() {
    }
}
