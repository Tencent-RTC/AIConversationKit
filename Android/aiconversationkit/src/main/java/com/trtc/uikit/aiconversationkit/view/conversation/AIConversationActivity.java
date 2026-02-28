package com.trtc.uikit.aiconversationkit.view.conversation;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.tencent.qcloud.tuicore.util.TUIBuild;
import com.trtc.tuikit.common.system.ContextProvider;
import com.trtc.uikit.aiconversationkit.AIConversationView;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.common.Logger;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;
import com.trtc.uikit.aiconversationkit.store.AIConversationConfig;
import com.trtc.uikit.aiconversationkit.store.AIStatus;
import com.trtc.uikit.aiconversationkit.view.feedback.AIConversationFeedbackActivity;

import java.io.Serializable;

public class AIConversationActivity extends AppCompatActivity {
    private static final String TAG = "AIConversationAy";

    public static final String KEY_START_AI_CONVERSATION = "KEY_START_AI_CONVERSATION";

    private AIConversationView aiConversationView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate : " + this);
        ContextProvider.setApplicationContext(this);
        initStatusBar();
        initView();
        startAIConversation();
        observeAIStatus();
        handleInterDemo();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "onDestroy : " + this);
    }

    @Override
    public void onBackPressed() {
    }

    private void initView() {
        setContentView(R.layout.conversation_activity_ai_conversation_main);
        aiConversationView = findViewById(R.id.conversation_view_ai_conversation);
        aiConversationView.setBackgroundImage(0);
        findViewById(R.id.conversation_cl_main_panel).setBackgroundResource(R.drawable.conversation_bg_full_screen);
    }

    private void startAIConversation() {
        AIConversationConfig config = parseAIConversationConfig();
        aiConversationView.startAIConversation(config, null);
    }

    private AIConversationConfig parseAIConversationConfig() {
        Intent intent = getIntent();
        Serializable start = intent.getSerializableExtra(KEY_START_AI_CONVERSATION);
        if (start == null) {
            Logger.e(TAG, "intent could not find KEY_START_AI_CONVERSATION");
            return null;
        }
        if (!(start instanceof AIConversationConfig)) {
            Logger.e(TAG, "key KEY_START_AI_CONVERSATION is not instanceof AIConversationConfig");
            return null;
        }
        return (AIConversationConfig) start;
    }

    private void observeAIStatus() {
        AIConversationLiveData.aiStatusLiveData().observe(this, status -> {
            Log.i(TAG, "aiStatus changed: " + status);
            if (status == AIStatus.OFFLINE) {
                Log.i(TAG, "aiStatus is OFFLINE, finishing activity");
                if (!PackageService.isInternalDemo()) {
                    return;
                }
                if (ConversationManager.sharedInstance().remainingExperienceTimeS.get() <= 0) {
                    showTimeoutDialog();
                    return;
                }
                goFeedback();
                finish();
            }
        });
    }

    private void showTimeoutDialog() {
        ExperienceTimeoutDialogFragment fragment = new ExperienceTimeoutDialogFragment();
        boolean isNeedFeedback = ConversationManager.sharedInstance().isNeedFeedback;
        fragment.showDialog(AIConversationActivity.this, "ExperienceTimeoutDialogFragment", isNeedFeedback);
    }

    private void goFeedback() {
        Intent intent = new Intent(AIConversationActivity.this, AIConversationFeedbackActivity.class);
        intent.putExtra(ConversationConstant.KEY_IS_NEED_FEEDBACK,
                ConversationManager.sharedInstance().isNeedFeedback);
        startActivity(intent);
    }

    private void handleInterDemo() {
        if (!PackageService.isInternalDemo()) {
            return;
        }
        ConversationManager.sharedInstance().fetchFeedback();
        ConstraintLayout experienceViewContainer = findViewById(R.id.conversation_cl_tip);
        experienceViewContainer.setVisibility(View.VISIBLE);
    }

    @SuppressLint("NewApi")
    private void initStatusBar() {
        Window window = getWindow();
        int sdkVersion = TUIBuild.getVersionInt();
        if (sdkVersion >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView()
                    .setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
        } else if (sdkVersion >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }
    }
}
