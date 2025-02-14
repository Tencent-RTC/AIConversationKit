package com.trtc.uikit.aiconversationkit.view.conversation;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;

import com.trtc.tuikit.common.livedata.Observer;
import com.trtc.uikit.aiconversationkit.manager.ConversationManager;
import com.trtc.uikit.aiconversationkit.state.ConversationState;

public class DialogInterruptView extends View {
    private final Observer<ConversationState.AIStatus> mAIStatusObserver = this::updateView;

    public DialogInterruptView(Context context) {
        this(context, null);
    }

    public DialogInterruptView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOnClickListener(v -> ConversationManager.sharedInstance().interruptConversation());
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

    private void updateView(ConversationState.AIStatus status) {
        setEnabled(status == ConversationState.AIStatus.SPEAKING);
    }
}
