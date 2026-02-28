package com.trtc.uikit.aiconversationkit.manager;

import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.tuikit.common.livedata.LiveData;
import com.trtc.uikit.aiconversationkit.common.Logger;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;
import com.trtc.uikit.aiconversationkit.manager.net.ClientFeedbackRequest;
import com.trtc.uikit.aiconversationkit.manager.net.FeedbackRequest;
import com.trtc.uikit.aiconversationkit.manager.net.ServerFeedbackRequest;

import java.util.Map;

public class ConversationManager {
    private static final    String              TAG         = "ConversationManager";
    private static volatile ConversationManager sInstance;

    private FeedbackRequest mFeedbackRequest = new ClientFeedbackRequest();

    public boolean           isNeedFeedback           = false;
    public LiveData<Integer> remainingExperienceTimeS = new LiveData<>(10 * 60);

    private ConversationManager() {
        if (PackageService.isInternalDemo()) {
            mFeedbackRequest = new ServerFeedbackRequest();
        }
        Logger.i(TAG, String.format("sharedInstance sInstance:%s", this));
    }

    public static ConversationManager sharedInstance() {
        if (sInstance == null) {
            synchronized (ConversationManager.class) {
                if (sInstance == null) {
                    sInstance = new ConversationManager();
                }
            }
        }
        return sInstance;
    }

    public void destroySharedInstance() {
        Logger.i(TAG, String.format("destroySharedInstance sInstance:%s", sInstance));
        sInstance = null;
    }

    public void fetchFeedback() {
        Logger.i(TAG, "fetchFeedback");
        mFeedbackRequest.fetchRemainingExperienceTime();
        mFeedbackRequest.fetchFeedback();
    }

    public void deductionExperienceTime() {
        mFeedbackRequest.deductionExperienceTime();
    }

    public void uploadFeedback(Map<String, String> feedback, TUIServiceCallback callback) {
        mFeedbackRequest.uploadFeedback(feedback, callback);
    }
}
