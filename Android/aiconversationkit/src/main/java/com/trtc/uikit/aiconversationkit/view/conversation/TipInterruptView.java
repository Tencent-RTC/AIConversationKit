package com.trtc.uikit.aiconversationkit.view.conversation;

import static com.trtc.uikit.aiconversationkit.view.ConversationConstant.INTERRUPT_MODE_SMART;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.R;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

public class TipInterruptView extends FrameLayout {
    private final Observer<ConversationState.AIStatus> mAIStatusObserver      = this::updateViewVisibility;
    private final Observer<Integer>                    mInterruptModeObserver = this::updateTipContent;

    private final TextView mTvInterruptTip;

    public TipInterruptView(@NonNull Context context) {
        this(context, null);
    }

    public TipInterruptView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.conversation_view_tip_interrupt, this);
        mTvInterruptTip = findViewById(R.id.conversation_tv_interrupt);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ConversationState conversationState = ConversationManager.sharedInstance().getConversationState();
        conversationState.aiStatus.observe(mAIStatusObserver);
        conversationState.interruptMode.observe(mInterruptModeObserver);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        ConversationState conversationState = ConversationManager.sharedInstance().getConversationState();
        conversationState.aiStatus.removeObserver(mAIStatusObserver);
        conversationState.interruptMode.removeObserver(mInterruptModeObserver);
    }

    private void updateViewVisibility(ConversationState.AIStatus aiStatus) {
        setVisibility(aiStatus == ConversationState.AIStatus.SPEAKING ? VISIBLE : INVISIBLE);
    }

    private void updateTipContent(int mode) {
        int interruptTipTextId = mode == INTERRUPT_MODE_SMART ? R.string.conversation_speech_to_interrupt
                : R.string.conversation_tap_to_interrupt;
        mTvInterruptTip.setText(interruptTipTextId);
    }
}
