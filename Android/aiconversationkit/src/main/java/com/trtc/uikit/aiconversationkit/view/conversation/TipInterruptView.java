package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

public class TipInterruptView extends FrameLayout {
    private final Observer<ConversationState.AIStatus> mAIStatusObserver = this::updateView;

    public TipInterruptView(@NonNull Context context) {
        this(context, null);
    }

    public TipInterruptView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.conversation_view_tip_interrupt, this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationManager.sharedInstance().getConversationState().aiStatus.observe(mAIStatusObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationManager.sharedInstance().getConversationState().aiStatus.removeObserver(mAIStatusObserver);
    }

    private void updateView(ConversationState.AIStatus aiStatus) {
        setVisibility(aiStatus == ConversationState.AIStatus.SPEAKING ? VISIBLE : INVISIBLE);
    }
}
