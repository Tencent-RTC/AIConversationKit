package com.trtc.uikit.aiconversationkit.view.feedback;

import static com.trtc.uikit.aiconversationkit.view.conversation.ConversationConstant.KEY_IS_NEED_FEEDBACK;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.view.conversation.ConversationConstant;

import java.util.HashMap;
import java.util.Map;

public class AIConversationFeedbackActivity extends AppCompatActivity {

    private Button           mBtnUpload;
    private EditText         mEtSuggestion;
    private FeedbackStarView mViewCallLatency;
    private FeedbackStarView mViewNoiseSuppression;
    private FeedbackStarView mViewAIResponse;
    private FeedbackStarView mViewInteractiveExperience;

    private Integer mSurveyDialogSatisfaction    = ConversationConstant.FEEDBACK_UNDO;
    private Integer mSurveyCallLatency           = ConversationConstant.FEEDBACK_UNDO;
    private Integer mSurveyNoiseSuppression      = ConversationConstant.FEEDBACK_UNDO;
    private Integer mSurveyAIResponse            = ConversationConstant.FEEDBACK_UNDO;
    private Integer mSurveyInteractiveExperience = ConversationConstant.FEEDBACK_UNDO;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.conversation_activity_ai_conversation_feedback);
        initView();
        updateViewBySatisfaction(ConversationConstant.FEEDBACK_UNDO);
    }

    private void initView() {
        boolean isNeedFeedback = getIntent().getBooleanExtra(KEY_IS_NEED_FEEDBACK, false);
        if (isNeedFeedback) {
            findViewById(R.id.conversation_btn_skip_feedback).setVisibility(View.GONE);
        } else {
            findViewById(R.id.conversation_btn_skip_feedback).setOnClickListener(v -> finish());
        }
        mBtnUpload = findViewById(R.id.conversation_btn_upload_feedback);
        mBtnUpload.setOnClickListener(v -> uploadFeedback());

        DialogSatisfactionView viewDialogSatisfaction = findViewById(R.id.conversation_view_dialog_satisfaction);
        viewDialogSatisfaction.setSurveyCallback(feedback -> {
            mSurveyDialogSatisfaction = feedback;
            updateViewBySatisfaction(feedback);
        });

        mViewCallLatency = findViewById(R.id.conversation_view_call_latency);
        mViewCallLatency.setSurveyCallback(feedback -> mSurveyCallLatency = feedback);
        mViewNoiseSuppression = findViewById(R.id.conversation_view_noise_suppression);
        mViewNoiseSuppression.setSurveyCallback(feedback -> mSurveyNoiseSuppression = feedback);
        mViewAIResponse = findViewById(R.id.conversation_view_ai_response);
        mViewAIResponse.setSurveyCallback(feedback -> mSurveyAIResponse = feedback);
        mViewInteractiveExperience = findViewById(R.id.conversation_view_interactive_experience);
        mViewInteractiveExperience.setSurveyCallback(feedback -> mSurveyInteractiveExperience = feedback);
        mEtSuggestion = findViewById(R.id.conversation_view_enter_suggestions);
    }

    private void updateViewBySatisfaction(int satisfaction) {
        boolean isReady = satisfaction != ConversationConstant.FEEDBACK_UNDO;
        mBtnUpload.setEnabled(isReady);
        mBtnUpload.setBackgroundResource(isReady ? R.drawable.conversation_bg_upload_feedback
                : R.drawable.conversation_bg_upload_feedback_disabled);
        mEtSuggestion.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mViewCallLatency.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mViewNoiseSuppression.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mViewAIResponse.setVisibility(isReady ? View.VISIBLE : View.GONE);
        mViewInteractiveExperience.setVisibility(isReady ? View.VISIBLE : View.GONE);
    }

    private void uploadFeedback() {
        Map<String, String> feedbackMap = new HashMap<>();
        feedbackMap.put("entirety", Integer.toString(mSurveyDialogSatisfaction));
        feedbackMap.put("callDelay", Integer.toString(mSurveyCallLatency));
        feedbackMap.put("noiseReduce", Integer.toString(mSurveyNoiseSuppression));
        feedbackMap.put("ai", Integer.toString(mSurveyAIResponse));
        feedbackMap.put("interaction", Integer.toString(mSurveyInteractiveExperience));
        feedbackMap.put("feedback", mEtSuggestion.getText().toString());
        ConversationManager.sharedInstance().uploadFeedback(feedbackMap, new TUIServiceCallback() {
            @Override
            public void onServiceCallback(int errorCode, String errorMessage, Bundle bundle) {
                ConversationManager.sharedInstance().destroySharedInstance();
                finish();
            }
        });
    }

    @Override
    public void onBackPressed() {
    }
}
