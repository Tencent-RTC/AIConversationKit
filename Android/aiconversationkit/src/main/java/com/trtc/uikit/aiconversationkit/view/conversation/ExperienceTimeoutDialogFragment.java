package com.trtc.uikit.aiconversationkit.view.conversation;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.tencent.qcloud.tuicore.TUICore;
import com.tencent.qcloud.tuicore.interfaces.TUIServiceCallback;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;
import com.trtc.uikit.aiconversationkit.view.feedback.AIConversationFeedbackActivity;

public class ExperienceTimeoutDialogFragment extends DialogFragment {
    private static final String TAG = "ExperienceTimeoutDF";

    private boolean mIsNeedFeedback = false;

    public void showDialog(@NonNull Context context, @Nullable String tag, boolean isNeedFeedback) {
        if (!(context instanceof FragmentActivity)) {
            Log.e(TAG, "context is not instance of FragmentActivity");
            return;
        }
        FragmentManager manager = ((FragmentActivity) context).getSupportFragmentManager();
        Fragment fragment = manager.findFragmentByTag(tag);
        if (fragment instanceof DialogFragment) {
            return;
        }
        setCancelable(false);
        mIsNeedFeedback = isNeedFeedback;
        this.show(manager, tag);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            Window window = dialog.getWindow();
            if (window != null) {
                window.setBackgroundDrawableResource(android.R.color.transparent);
                window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
                window.setDimAmount(0.9f);
            }
        }
        return inflater.inflate(R.layout.conversation_dialog_experience_timeout, container, true);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        initCloseView(view);
        initContactUsView(view);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void initCloseView(View root) {
        Button btnClose = root.findViewById(R.id.conversation_btn_close_dialog);
        btnClose.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            finishActivity();
            Intent intent = new Intent(getContext(), AIConversationFeedbackActivity.class);
            intent.putExtra(ConversationConstant.KEY_IS_NEED_FEEDBACK, mIsNeedFeedback);
            startActivity(intent);
        });
    }

    private void initContactUsView(View root) {
        Button btnContactUs = root.findViewById(R.id.conversation_btn_contact_us);
        btnContactUs.setOnClickListener(v -> {
            dismissAllowingStateLoss();
            finishActivity();
            goContactUsPage();
        });
    }

    private void finishActivity() {
        Context context = getContext();
        if (!(context instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) context;
        activity.finish();
    }

    private void goContactUsPage() {
        TUIServiceCallback serviceCallback = new TUIServiceCallback() {
            @Override
            public void onServiceCallback(int errorCode, String errorMessage, Bundle bundle) {
                boolean isRTCCube = bundle.getBoolean("paramIsRTCCUbe", true);
                if (isRTCCube) {
                    Uri webPage = Uri.parse(ConversationConstant.DOMESTIC_CONTACT_US_URL);
                    Intent intent = new Intent(Intent.ACTION_VIEW, webPage);
                    if (intent.resolveActivity(getContext().getPackageManager()) != null) {
                        startActivity(intent);
                    }
                } else {
                    TUICore.startActivity("ContactActivity", null);
                }
            }
        };
        TUICore.callService("ConfigureService", "methodGetIsRTCCUbe", null, serviceCallback);
    }
}
