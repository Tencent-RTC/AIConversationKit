package com.trtc.uikit.aiconversationkit.view.feedback;

import static com.trtc.uikit.aiconversationkit.view.ConversationConstant.KEY_IS_NEED_FEEDBACK;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;

import java.util.HashMap;
import java.util.Map;

public class AIConversationFeedbackActivity extends AppCompatActivity {

    private EditText mEtSuggestion;

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
    }

    private void initView() {
        boolean isNeedFeedback = getIntent().getBooleanExtra(KEY_IS_NEED_FEEDBACK, false);
        if (isNeedFeedback) {
            findViewById(R.id.conversation_btn_skip_feedback).setVisibility(View.GONE);
        } else {
            findViewById(R.id.conversation_btn_skip_feedback).setOnClickListener(v -> finish());
        }
        Button btnUpload = findViewById(R.id.conversation_btn_upload_feedback);
        btnUpload.setEnabled(false);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uploadFeedback();
            }
        });
        DialogSatisfactionView viewDialogSatisfaction = findViewById(R.id.conversation_view_dialog_satisfaction);
        viewDialogSatisfaction.setSurveyCallback(new SurveyCallback() {
            @Override
            public void onSurvey(int feedback) {
                mSurveyDialogSatisfaction = feedback;
                enableUploadIfNeeded(btnUpload);
            }
        });
        FeedbackStarView viewCallLatency = findViewById(R.id.conversation_view_call_latency);
        viewCallLatency.setSurveyCallback(new SurveyCallback() {
            @Override
            public void onSurvey(int feedback) {
                mSurveyCallLatency = feedback;
                enableUploadIfNeeded(btnUpload);
            }
        });
        FeedbackStarView viewNoiseSuppression = findViewById(R.id.conversation_view_noise_suppression);
        viewNoiseSuppression.setSurveyCallback(new SurveyCallback() {
            @Override
            public void onSurvey(int feedback) {
                mSurveyNoiseSuppression = feedback;
                enableUploadIfNeeded(btnUpload);
            }
        });
        FeedbackStarView viewAIResponse = findViewById(R.id.conversation_view_ai_response);
        viewAIResponse.setSurveyCallback(new SurveyCallback() {
            @Override
            public void onSurvey(int feedback) {
                mSurveyAIResponse = feedback;
                enableUploadIfNeeded(btnUpload);
            }
        });
        FeedbackStarView viewInteractiveExperience = findViewById(R.id.conversation_view_interactive_experience);
        viewInteractiveExperience.setSurveyCallback(new SurveyCallback() {
            @Override
            public void onSurvey(int feedback) {
                mSurveyInteractiveExperience = feedback;
                enableUploadIfNeeded(btnUpload);
            }
        });
        mEtSuggestion = findViewById(R.id.conversation_view_enter_suggestions);
    }

    private void enableUploadIfNeeded(Button btnUpload) {
        if (!isEnableUploadFeedback()) {
            return;
        }
        btnUpload.setEnabled(true);
        btnUpload.setBackgroundResource(R.drawable.conversation_bg_upload_feedback);
    }

    private boolean isEnableUploadFeedback() {
        if (mSurveyDialogSatisfaction == ConversationConstant.FEEDBACK_UNDO) {
            return false;
        }
        if (mSurveyCallLatency == ConversationConstant.FEEDBACK_UNDO) {
            return false;
        }
        if (mSurveyNoiseSuppression == ConversationConstant.FEEDBACK_UNDO) {
            return false;
        }
        if (mSurveyAIResponse == ConversationConstant.FEEDBACK_UNDO) {
            return false;
        }
        return mSurveyInteractiveExperience != ConversationConstant.FEEDBACK_UNDO;
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
