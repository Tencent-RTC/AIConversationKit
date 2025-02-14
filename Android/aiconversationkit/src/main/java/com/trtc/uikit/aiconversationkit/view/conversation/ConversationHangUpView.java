package com.trtc.uikit.aiconversationkit.view.conversation;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.utils.widget.ImageFilterView;

import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.manager.internal.PackageService;
import com.trtc.uikit.aiconversationkit.view.ConversationConstant;
import com.trtc.uikit.aiconversationkit.view.feedback.AIConversationFeedbackActivity;

public class ConversationHangUpView extends FrameLayout {
    private final Context mActivityContext;

    public ConversationHangUpView(@NonNull Context context) {
        this(context, null);
    }

    public ConversationHangUpView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        mActivityContext = context;
        initView(context);
    }

    private void initView(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_media_operation, this);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.conversation_ic_hang_up);
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        ((ImageFilterView) findViewById(R.id.conversation_ifv_operation)).setBackgroundDrawable(drawable);
        ((TextView)findViewById(R.id.conversation_tv_operation)).setText(R.string.conversation_hang_up);

        root.setOnClickListener(v -> {
            goFeedback();
            ConversationManager.sharedInstance().stopConversation();
            finishActivity();
        });
    }

    private void goFeedback() {
        if (!PackageService.isInternalDemo()) {
            return;
        }
        Intent intent = new Intent(getContext(), AIConversationFeedbackActivity.class);
        intent.putExtra(ConversationConstant.KEY_IS_NEED_FEEDBACK,
                ConversationManager.sharedInstance().getConversationState().isNeedFeedback);
        mActivityContext.startActivity(intent);
    }

    private void finishActivity() {
        if (!(mActivityContext instanceof Activity)) {
            return;
        }
        Activity activity = (Activity) mActivityContext;
        activity.finish();
    }
}
