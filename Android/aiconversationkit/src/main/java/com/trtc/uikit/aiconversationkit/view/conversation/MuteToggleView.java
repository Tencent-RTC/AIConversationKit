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

public class MuteToggleView extends FrameLayout {
    private ImageFilterView mIfvAudio;
    private TextView        mTvAudio;

    private final Observer<Boolean> mMuteObserver = this::updateView;

    public MuteToggleView(@NonNull Context context) {
        this(context, null);
    }

    public MuteToggleView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().isAudioMuted.observe(mMuteObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().isAudioMuted.removeObserver(mMuteObserver);
    }

    private void initView(Context context) {
        View root = LayoutInflater.from(context).inflate(R.layout.conversation_view_media_operation, this);
        mIfvAudio = findViewById(R.id.conversation_ifv_operation);
        mTvAudio = findViewById(R.id.conversation_tv_operation);

        root.setOnClickListener(v -> ConversationManager.sharedInstance().toggleLocalAudio());
    }

    private void updateView(boolean isMute) {
        int bgResId = isMute ? R.drawable.conversation_ic_audio_state_mute
                : R.drawable.conversation_ic_audio_state_unmute;
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), bgResId);
        Drawable drawable = new BitmapDrawable(getResources(), bitmap);
        mIfvAudio.setBackgroundDrawable(drawable);
        mTvAudio.setText(isMute ? R.string.conversation_mic_unmute : R.string.conversation_mic_mute);
    }
}
