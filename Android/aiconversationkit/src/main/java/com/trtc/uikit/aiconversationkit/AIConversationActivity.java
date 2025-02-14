package com.trtc.uikit.aiconversationkit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.trtc.tuikit.common.system.ContextProvider;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;

import java.io.Serializable;

public class AIConversationActivity extends AppCompatActivity {
    private static final String TAG = "AIConversationActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        ContextProvider.setApplicationContext(this);
        startAIConversation();
        super.onCreate(savedInstanceState);

        setContentView(R.layout.conversation_activity_ai_conversation_main);
        if (PackageService.isInternalDemo()) {
            ConstraintLayout experienceViewContainer = findViewById(R.id.conversation_cl_tip);
            experienceViewContainer.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
    }

    private void startAIConversation() {
        AIConversationDefine.StartAIConversationParams params = parseStartAIConversationParams();
        if (params == null) {
            Log.e(TAG, "The parameter StartAIConversationParams is required.");
            finish();
            return;
        }
        ConversationManager.sharedInstance().startConversation(params);
    }

    private AIConversationDefine.StartAIConversationParams parseStartAIConversationParams() {
        Intent intent = getIntent();
        Serializable start = intent.getSerializableExtra(AIConversationDefine.KEY_START_AI_CONVERSATION);
        if (start == null) {
            Log.e(TAG, "intent can not found KEY_START_AI_CONVERSATION");
            return null;
        }
        if (!(start instanceof AIConversationDefine.StartAIConversationParams)) {
            Log.e(TAG, "key KEY_START_AI_CONVERSATION is not instanceof StartAIConversationParams");
            return null;
        }
        AIConversationDefine.StartAIConversationParams params = (AIConversationDefine.StartAIConversationParams) start;
        Log.i(TAG, "parseStartAIConversationParams : " + params);
        return params;
    }
}
