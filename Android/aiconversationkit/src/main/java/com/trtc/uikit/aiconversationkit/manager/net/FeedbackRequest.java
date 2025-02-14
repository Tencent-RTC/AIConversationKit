package com.trtc.uikit.aiconversationkit.manager.net;

import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;

import java.util.Map;

public interface FeedbackRequest {
    void uploadFeedback(Map<String, String> param, TUIServiceCallback callback);

    void fetchFeedback();

    void fetchRemainingExperienceTime();

    void deductionExperienceTime();
}
