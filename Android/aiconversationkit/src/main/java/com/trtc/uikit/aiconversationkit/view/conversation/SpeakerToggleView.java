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

public class SpeakerToggleView extends FrameLayout {
    private ImageFilterView mIfvSpeaker;
    private TextView        mTvSpeaker;

    private final Observer<Boolean> mSpeakerObserver = this::updateView;

    public SpeakerToggleView(@NonNull Context context) {
        this(context, null);
    }

    public SpeakerToggleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().isSpeakerOpened.observe(mSpeakerObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().isSpeakerOpened.removeObserver(mSpeakerObserver);
    }

    private void initView(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_media_operation, this);
        mIfvSpeaker = findViewById(R.id.conversation_ifv_operation);
        mTvSpeaker = findViewById(R.id.conversation_tv_operation);

        root.setOnClickListener(v -> ConversationManager.sharedInstance().toggleSpeaker());
    }

    private void updateView(boolean isOpenSpeaker) {
        int bgResId = isOpenSpeaker ? R.drawable.conversation_ic_speaker_state_open
                : R.drawable.conversation_ic_speaker_state_close;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bgResId);
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        mIfvSpeaker.setBackgroundDrawable(drawable);
        mTvSpeaker.setText(isOpenSpeaker ? R.string.conversation_close_speaker : R.string.conversation_open_speaker);
    }
}
