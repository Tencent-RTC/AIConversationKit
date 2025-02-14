package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
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

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;

public class PauseAIAudioView extends FrameLayout {
    private ImageFilterView mIfvPause;
    private TextView        mTvPause;

    private final Observer<Boolean> mPauseObserver = this::updateView;

    public PauseAIAudioView(@NonNull Context context) {
        this(context, null);
    }

    public PauseAIAudioView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().isPaused.observe(mPauseObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().isPaused.removeObserver(mPauseObserver);
    }

    private void initView(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_media_operation, this);
        mIfvPause = findViewById(R.id.conversation_ifv_operation);
        mTvPause = findViewById(R.id.conversation_tv_operation);

        root.setOnClickListener(v -> ConversationManager.sharedInstance().toggleAIAudio());
    }

    private void updateView(boolean isAIPause) {
        int bgResId = isAIPause ? R.drawable.conversation_ic_ai_state_pause
                : R.drawable.conversation_ic_ai_state_resume;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bgResId);
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        mIfvPause.setBackgroundDrawable(drawable);
        mTvPause.setText(isAIPause ? R.string.conversation_continue : R.string.conversation_pause);
    }
}
