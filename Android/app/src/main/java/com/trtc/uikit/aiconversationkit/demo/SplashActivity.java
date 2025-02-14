package com.trtc.uikit.aiconversationkit.demo;

import static com.trtc.uikit.aiconversationkit.AIConversationDefine.KEY_START_AI_CONVERSATION;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.tencent.qcloud.tuicore.TUILogin;
import com.tencent.qcloud.tuicore.interfaces.TUICallback;
import com.trtc.uikit.aiconversationkit.AIConversationActivity;
import com.trtc.uikit.aiconversationkit.AIConversationDefine;

public class SplashActivity extends Activity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!isTaskRoot() && getIntent() != null && getIntent().hasCategory(Intent.CATEGORY_LAUNCHER)
                && Intent.ACTION_MAIN.equals(getIntent().getAction())) {
            finish();
            return;
        }
        quickStartAIConversation();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(TAG, "onNewIntent: intent -> " + intent.getData());
        setIntent(intent);
        quickStartAIConversation();
    }

    private void quickStartAIConversation() {
        if (!Config.isConfigAvailable()) {
            Toast.makeText(this.getApplicationContext(), R.string.app_toast_error_empty_params, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        int sdkAppId = Config.getSdkAppId();
        String userId = Config.getUserId();
        String userSig = Config.getUserSig();
        Log.d(TAG, "quickStartAIConversation TUILogin.login sdkAppId=" + sdkAppId + " userId=" + userId);
        TUILogin.login(this.getApplicationContext(), sdkAppId, userId, userSig, new TUICallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "TUILogin.login onSuccess");
                AIConversationDefine.StartAIConversationParams params = Config.parseStartAIConversationParams(
                        Config.SECRET_ID, Config.SECRET_KEY, Config.CONFIG);
                Intent intent = new Intent(SplashActivity.this, AIConversationActivity.class);
                intent.putExtra(KEY_START_AI_CONVERSATION, params);
                startActivity(intent);
                finish();
            }

            @Override
            public void onError(int errorCode, String errorMessage) {
                Log.d(TAG, "TUILogin.login onError errorCode=" + errorCode + " errorMessage=" + errorMessage);
            }
        });
    }
}
