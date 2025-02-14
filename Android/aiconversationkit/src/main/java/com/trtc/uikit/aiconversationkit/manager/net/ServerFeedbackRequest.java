package com.trtc.uikit.aiconversationkit.manager.net;

import android.os.Bundle;
import android.util.Log;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

import java.util.HashMap;
import java.util.Map;

public class ServerFeedbackRequest implements FeedbackRequest {
    private static final String TAG = "ServerFeedbackReq";

    @Override
    public void uploadFeedback(Map<String, String> param, TUIServiceCallback callback) {
        Log.d(TAG, "uploadFeedback");
        Map<String, Object> feedback = new HashMap<>();
        feedback.put("entirety", param.get("entirety"));
        feedback.put("callDelay", param.get("callDelay"));
        feedback.put("noiseReduce", param.get("noiseReduce"));
        feedback.put("ai", param.get("ai"));
        feedback.put("interaction", param.get("interaction"));
        feedback.put("feedback", param.get("feedback"));
        TUICore.callService("AIConversationService",
                "methodUploadFeedback", feedback, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        Log.d(TAG, String.format("uploadFeedback code=%d message==%s", code, message));
                        if (callback != null) {
                            callback.onServiceCallback(code, message, bundle);
                        }
                    }
                });
    }

    @Override
    public void fetchFeedback() {
        Log.d(TAG, "fetchFeedback");
        TUICore.callService("AIConversationService",
                "methodFetchFeedback", null, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        if (code != 0) {
                            Log.e(TAG, String.format("fetchFeedback code=%d message==%s", code, message));
                            return;
                        }
                        ConversationState state = ConversationManager.sharedInstance().getConversationState();
                        state.isNeedFeedback = bundle.getBoolean("isNeedFeedback");
                        Log.d(TAG, String.format("fetchFeedback code=%d isNeedFeedback==%s",
                                code, state.isNeedFeedback));
                    }
                });
    }

    @Override
    public void fetchRemainingExperienceTime() {
        Log.d(TAG, "fetchRemainingExperienceTime");
        TUICore.callService("AIConversationService",
                "methodFetchRemainingExperienceTime", null, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        if (code != 0) {
                            Log.e(TAG, String.format("fetchRemainingExperienceTime code=%d message==%s",
                                    code, message));
                            return;
                        }
                        String terminal = bundle.getString("terminal");
                        int remainingExperienceTime;
                        try {
                            remainingExperienceTime = Integer.parseInt(terminal);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, String.format("fetchRemainingExperienceTime NumberFormatException terminal=%s",
                                    terminal));
                            return;
                        }
                        Log.d(TAG, String.format("fetchRemainingExperienceTime code=%d terminal==%d",
                                code, remainingExperienceTime));
                        ConversationState state = ConversationManager.sharedInstance().getConversationState();
                        state.remainingExperienceTimeS.set(remainingExperienceTime);
                    }
                });
    }

    @Override
    public void deductionExperienceTime() {
        Log.d(TAG, "deductionExperienceTime");
        TUICore.callService("AIConversationService",
                "methodDeductionExperienceTime", null, new TUIServiceCallback() {
                    @Override
                    public void onServiceCallback(int code, String message, Bundle bundle) {
                        Log.d(TAG, String.format("deductionExperienceTime code=%d message==%s", code, message));
                    }
                });
    }
}
